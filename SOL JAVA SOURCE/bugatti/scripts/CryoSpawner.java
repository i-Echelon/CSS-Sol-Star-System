package bugatti.scripts;

import bugatti.utils.ModCheck;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CryoSpawner {
    private static final boolean HAS_RAT = ModCheck.hasRAT;
    public static final String KEY_INMATES = "$sol_inmates";
    public static final String KEY_RAPSHEET = "$sol_inmate_rapsheet";

    static class SolSkill {
        String id;
        int weight;
        boolean hasElite; // whether this skill can be elite

        SolSkill(String id, int weight) {
            this.id = id;
            this.weight = weight;
            this.hasElite = false;
        }

        SolSkill(String id, int weight, boolean hasElite) {
            this.id = id;
            this.weight = weight;
            this.hasElite = hasElite;
        }
    }
    // SKILL POOLS
    private static final Map<String, List<SolSkill>> SKILL_POOLS = new HashMap<>();
    static {
        SKILL_POOLS.put("RAT", Arrays.asList(
                new SolSkill("rat_perfect_planning", 5),
                new SolSkill("rat_maintaining_momentum", 4),
                new SolSkill("rat_maverick", 3),
                new SolSkill("rat_auto_engineer", 2)
        ));
        SKILL_POOLS.put("COMBAT", Arrays.asList(
                new SolSkill("helmsmanship", 5, true),
                new SolSkill("combat_endurance", 5, true),
                new SolSkill("impact_mitigation", 4, true),
                new SolSkill("damage_control", 3, true),
                new SolSkill("field_modulation", 3, true),
                new SolSkill("point_defense", 3, true),
                new SolSkill("target_analysis", 3, true),
                new SolSkill("ballistic_mastery", 3, true),
                new SolSkill("systems_expertise", 3, true),
                new SolSkill("missile_specialization", 3, true),
                //TECH
                new SolSkill("gunnery_implants", 4, true),
                new SolSkill("energy_weapon_mastery", 3, true),
                //INDUSTRY
                new SolSkill("ordnance_expert", 4, true),
                new SolSkill("polarized_armor", 4, true)
        ));
    }
    // persona weight multiplier
    private static float getPersonalityMp(PersonAPI p, SolSkill skill) {
        // Aggressive + Reckless officers like offense; Timid prefers defense.
        String per = p.getPersonalityAPI().getId();

        switch (per) {
            case Personalities.AGGRESSIVE, Personalities.RECKLESS -> {
                // Bias toward offensive skills
                if (skill.id.equals("gunnery_implants") ||
                    skill.id.equals("ballistic_mastery") ||
                    skill.id.equals("energy_weapon_mastery") ||
                    skill.id.equals("target_analysis"))
                    return 1.6f;
                return 1.1f;
            }
            case Personalities.STEADY -> {
                return 1f;
            }
            case Personalities.CAUTIOUS -> {
                // Prefers defense systems
                if (skill.id.equals("impact_mitigation") ||
                    skill.id.equals("polarized_armor") ||
                    skill.id.equals("damage_control") ||
                    skill.id.equals("point_defense"))
                    return 1.5f;
                return 0.9f;
            }
            case Personalities.TIMID -> {
                // Very defensive
                if (skill.id.equals("impact_mitigation") ||
                    skill.id.equals("polarized_armor") ||
                    skill.id.equals("point_defense"))
                    return 1.7f;
                return 0.7f;
            }
            default -> {
                return 1.0f;
            }
        }
    }
    private static SolSkill pickWeightedSkill(PersonAPI p, List<SolSkill> pool, Set<String> alreadyChosen) {
        WeightedRandomPicker<SolSkill> picker = new WeightedRandomPicker<>();

        for (SolSkill s : pool) {
            if (!alreadyChosen.contains(s.id)) {
                float personalityMod = getPersonalityMp(p, s);
                float finalWeight = s.weight * personalityMod;
                picker.add(s, finalWeight);
            }
        }

        return picker.isEmpty() ? null : picker.pick();
    }
    private static int determineSkillLevel(SolSkill skill, PersonAPI p) {
        if (!skill.hasElite) return 1;

        float chance;

        // Personality-based elite chance
        switch (p.getPersonalityAPI().getId()) {
            case Personalities.RECKLESS -> chance = 0.60f;
            case Personalities.AGGRESSIVE -> chance = 0.45f;
            case Personalities.STEADY -> chance = 0.30f;
            case Personalities.CAUTIOUS -> chance = 0.20f;
            case Personalities.TIMID -> chance = 0.10f;
            default -> chance = 0.25f;
        }

        float mult = getPersonalityMp(p, skill);
        chance *= Math.min(mult * 0.8f, 1f);

        return Math.random() < chance ? 2 : 1;
    }
    // Creates inmate data on a cryochamber entity.
    public static void setupCryochamber(CustomCampaignEntityAPI entity) {
        MemoryAPI mem = entity.getMemoryWithoutUpdate();
        if (mem.contains(KEY_INMATES)) return;

        List<PersonAPI> inmates = new ArrayList<>();
        Set<String> charges = new HashSet<>();

        for (int i = 0; i < 8; i++) {
            PersonAPI p = genInmate(charges);
            inmates.add(p);
        }

        mem.set(KEY_INMATES, inmates);
        mem.set("$sol_has_inmates", true);
    }
    // Generate inmates with reasonable defaults and randomized details.
    private static PersonAPI genInmate(Set<String> charges) {
        PersonAPI p = Global.getFactory().createPerson();
        p.setFaction("neutral");

        Gender gender = Math.random() < 0.5 ? Gender.MALE : Gender.FEMALE;
        p.setGender(gender);

        p.setPortraitSprite(pickRNGPortrait(gender));

        // NEW NAME FORMAT
        p.setName(genInmateID(gender));

        // Personality
        String[] personalities = {
                Personalities.TIMID, Personalities.CAUTIOUS, Personalities.STEADY,
                Personalities.AGGRESSIVE, Personalities.RECKLESS
        };
        p.setPersonality(personalities[(int)(Math.random() * personalities.length)]);

        // Skills - adjust based on whether RAT is installed
        if (HAS_RAT) {
            pickRATSkill(p);
            for (int i = 0; i < 4; i++) pickRNGSkills(p);
        } else {
            // Give 5 normal combat skills if RAT isn't installed
            for (int i = 0; i < 5; i++) pickRNGSkills(p);
        }

        p.getStats().setLevel(5);
        p.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_LEVEL, 7);

        // UNIQUE RAP SHEET
        String story = pickRNGRapSheet(charges);
        charges.add(story);
        p.getMemoryWithoutUpdate().set(KEY_RAPSHEET, story);

        return p;
    }
    private static FullName genInmateID(Gender gender) {
        String first = "Inmate";

        // Generate 7-character alphanumeric ID
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            sb.append(chars.charAt((int)(Math.random() * chars.length())));
        }

        String last = sb.toString();

        return new FullName(first, last, gender);
    }
    private static String pickRNGRapSheet(Set<String> charges) {
        List<String> available = RAP_SHEET.stream()
                .filter(story -> !charges.contains(story))
                .toList();

        if (available.isEmpty()) {
            return "[NO RECORDS REMAINING]";
        }

        return available.get((int)(Math.random() * available.size()));
    }
    private static String pickRNGPortrait(Gender gender) {
        WeightedRandomPicker<String> picker = Global.getSector()
                .getFaction("independent")
                .getPortraits(gender);

        if (picker == null || picker.isEmpty()) {
            return Global.getSettings().getSpriteName("characters", "generic");
        }

        return picker.pick();
    }
    private static void pickRATSkill(PersonAPI p) {
        if (!HAS_RAT) return; // Safety check

        List<SolSkill> pool = SKILL_POOLS.get("RAT");
        if (pool == null || pool.isEmpty()) return; // Extra safety

        // Track duplicates
        Set<String> used = p.getStats().getSkillsCopy().stream()
                .map(s -> s.getSkill().getId())
                .collect(Collectors.toSet());

        SolSkill picked = pickWeightedSkill(p, pool, used);
        if (picked == null) {
            // fallback: pick ANY RAT skill ignoring duplicates
            picked = pool.get(0);
        }

        // Verify the skill exists before trying to add it
        if (Global.getSettings().getSkillSpec(picked.id) != null) {
            p.getStats().setSkillLevel(picked.id, 1);
        }
    }
    private static void pickRNGSkills(PersonAPI p) {
        List<SolSkill> pool = SKILL_POOLS.get("COMBAT");

        // Track what skills the officer already has
        Set<String> used = p.getStats().getSkillsCopy().stream()
                .map(sl -> sl.getSkill().getId())
                .collect(Collectors.toSet());

        SolSkill picked = pickWeightedSkill(p, pool, used);
        if (picked == null) return;

        int skillLevel = determineSkillLevel(picked, p);

        p.getStats().setSkillLevel(picked.id, skillLevel);
    }

    // Flavor Text for rap sheets
    private static final List<String> RAP_SHEET = new ArrayList<>(Arrays.asList(
            "Mutiny under collapse conditions at Luna Shipyards.",
            "Destruction of a civilian freighter during a classified intercept.",
            "Desertion and theft of restricted FTL components.",
            "Negligent operation of autonomous strike drones leading to colony loss.",
            "Sabotage of a diplomatic convoy during the Jovian Gate Riots.",

            "Refusal to obey direct fire orders during civil unrest suppression.",
            "Trafficking of cryo-warden override codes to pirate organizations.",
            "Unauthorized military-grade cybernetic augmentation.",
            "Gross negligence leading to friendly fleet losses.",

            "Hijacking of relief shipments during early Collapse evacuations.",
            "Violent misconduct during transport of pre-Collapse data cores.",
            "Sedition and insubordination during Earth evacuation operations.",

            "Attempted unauthorized thaw and release of cryostorage inmates.",
            "Distribution of anti-Domain propaganda.",
            "Illegal AI manipulation and drone repurposing.",

            "Classified: Marked ‘Extremely Hazardous – Oversight Failure’.",
            "Classified: ‘Omega-Level Offender – Do Not Revive’.",
            "All records lost; previous logs cite severe threat potential.",

            "Sabotage of a Jovian Secession drop shuttle (evidence inconclusive).",
            "Participation in Titan Naval Yard mutinies; politically sensitive misconduct.",
            "Unauthorized rescue prioritization during Evacuation Fleet collapse.",
            "Insubordination and intelligence compromise during Luna extremist sting operation.",
            "Reactor AI tampering causing shipyard detonation.",
            "Violent instability following prohibited cognition-enhancement trials.",
            "Loyalty-code violation during Mars Garrison crackdowns.",
            "Integration of an Alpha-level AI core into a combat simulator.",
            "Suspected involvement in Neptune Deep Relay catastrophic FTL event.",
            "Designation as strategic dissident during pre-Collapse security purges."
    ));
}