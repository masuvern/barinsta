package awais.instagrabber.utils.emoji;

import android.util.Log;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.customviews.emoji.Emoji;
import awais.instagrabber.customviews.emoji.EmojiCategory;
import awais.instagrabber.customviews.emoji.EmojiCategoryType;

public final class EmojiParser {
    private static final String TAG = EmojiParser.class.getSimpleName();
    private static final Object LOCK = new Object();

    private static EmojiParser instance;

    // private static final String COMBINING_ENCLOSING_KEYCAP = "\u20E3";
    // private static final String ZWJ = "\u200D";
    // private static final UnicodeSet REGIONAL_INDICATORS = new UnicodeSet(0x1F1E6, 0x1F1FF).freeze();
    // private static final UnicodeSet TAGS = new UnicodeSet(0xE0000, 0xE007F).freeze();
    // private static final UnicodeSet FAMILY = new UnicodeSet("[\u200D 👦-👩 💋 ❤]").freeze();
    // private static final UnicodeSet GENDER = new UnicodeSet().add(0x2640).add(0x2642).freeze();
    // private static final UnicodeSet SPECIALS = new UnicodeSet("["
    //         + "{🐈‍⬛}{🐻‍❄}{👨‍🍼}{👩‍🍼}{🧑‍🍼}{🧑‍🎄}{🧑‍🤝‍🧑}{🏳‍🌈} {👁‍🗨} {🏴‍☠} {🐕‍🦺} {👨‍🦯} {👨‍🦼} {👨‍🦽} {👩‍🦯} {👩‍🦼} {👩‍🦽}"
    //         + "{🏳‍⚧}{🧑‍⚕}{🧑‍⚖}{🧑‍✈}{🧑‍🌾}{🧑‍🍳}{🧑‍🎓}{🧑‍🎤}{🧑‍🎨}{🧑‍🏫}{🧑‍🏭}{🧑‍💻}{🧑‍💼}{🧑‍🔧}{🧑‍🔬}{🧑‍🚀}{🧑‍🚒}{🧑‍🦯}{🧑‍🦼}{🧑‍🦽}"
    //         + "{❤‍🔥}, {❤‍🩹}, {😮‍💨}, {😵‍💫}" // #E13.1
    //         + "]").freeze();
    // May have to add from above, if there is a failure in testAnnotationPaths. Failure will be like:
    // got java.util.TreeSet<[//ldml/annotations/annotation[@cp="🏳‍⚧"][@type="tts"], //ldml/annotations/annotation[@cp="🧑‍⚕"][@type="tts"], ...
    // just extract the items in "...", and change into {...} for adding above.
    // Example: //ldml/annotations/annotation[@cp="🧑‍⚕"] ==> {🧑‍⚕}
    // private static final UnicodeSet MAN_WOMAN = new UnicodeSet("[👨 👩]").freeze();
    // private static final UnicodeSet OBJECT = new UnicodeSet("[👩 🎓 🌾 🍳 🏫 🏭 🎨 🚒 ✈ 🚀 🎤 💻 🔬 💼 🔧 ⚖ ⚕]").freeze();
    // private static final String TYPE_TTS = "[@type=\"tts\"]";
    private static final String EMOJI_VARIANT = "\uFE0F";
    private static final UnicodeSet SKIN_TONE_MODIFIERS = new UnicodeSet("[🏻-🏿]").freeze();
    private static final String SKIN_TONE_PATTERN = SKIN_TONE_MODIFIERS.toPattern(true);
    private static final Map<EmojiCategoryType, EmojiCategory> CATEGORY_MAP = new LinkedHashMap<>();
    private static final Map<String, Emoji> ALL_EMOJIS = new HashMap<>();

    // private final UnicodeMap<String> emojiToMajorCategory = new UnicodeMap<>();
    // private final UnicodeMap<String> emojiToMinorCategory = new UnicodeMap<>();
    // private final UnicodeMap<String> toName = new UnicodeMap<>();
    // /**
    //  * A mapping from a majorCategory to a unique ordering number, based on the first time it is encountered.
    //  */
    // private final Map<String, Long> majorToOrder = new HashMap<>();
    // private final List<String> majorToOrder = new LinkedList<String>();
    // /**
    //  * A mapping from a minorCategory to a unique ordering number, based on the first time it is encountered.
    //  */
    // private final Map<String, Long> minorToOrder = new HashMap<>();
    // private final Map<String, Long> emojiToOrder = new LinkedHashMap<>();
    // private final UnicodeSet nonConstructed = new UnicodeSet();
    // private final UnicodeSet allRgi = new UnicodeSet();
    // private final UnicodeSet allRgiNoES = new UnicodeSet();
    // private final UnicodeMap<String> EXTRA_SYMBOL_MINOR_CATEGORIES = new UnicodeMap<>();
    // private final Map<String, Long> EXTRA_SYMBOL_ORDER;
    // private final boolean DEBUG = false;
    // private Set<String> NAME_PATHS = null;
    // private Set<String> KEYWORD_PATHS = null;
    private ImmutableList<EmojiCategory> categories;

    public static EmojiParser getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new EmojiParser();
                }
            }
        }
        return instance;
    }

    private EmojiParser() {
        // Log.d(TAG, "Emoji: " + new Date());
        // String[][] data = {
        //         {"arrow", "→ ↓ ↑ ← ↔ ↕ ⇆ ⇅"},
        //         {"alphanum", "© ® ℗ ™ µ"},
        //         {"geometric", "▼ ▶ ▲ ◀ ● ○ ◯ ◊"},
        //         {"math", "× ÷ √ ∞ ∆ ∇ ⁻ ¹ ² ³ ≡ ∈ ⊂ ∩ ∪ ° + ± − = ≈ ≠ > < ≤ ≥ ¬ | ~"},
        //         {"punctuation", "§ † ‡ \\u0020  , 、 ، ; : ؛ ! ¡ ? ¿ ؟ ¶ ※ / \\ & # % ‰ ′ ″ ‴ @ * ♪ ♭ ♯ ` ´ ^ ¨ ‐ ― _ - – — • · . … 。 ‧ ・ ‘ ’ ‚ ' “ ” „ » « ( ) [ ] { } 〔 〕 〈 〉 《 》 「 」 『 』 〖 〗 【 】"},
        //         {"currency", "€ £ ¥ ₹ ₽ $ ¢ ฿ ₪ ₺ ₫ ₱ ₩ ₡ ₦ ₮ ৳ ₴ ₸ ₲ ₵ ៛ ₭ ֏ ₥ ₾ ₼ ₿ ؋"},
        //         {"other-symbol", "‾‽‸⁂↚↛↮↙↜↝↞↟↠↡↢↣↤↥↦↧↨↫↬↭↯↰↱↲↳↴↵↶↷↸↹↺↻↼↽↾↿⇀⇁⇂⇃⇄⇇⇈⇉⇊⇋⇌⇐⇍⇑⇒⇏⇓⇔⇎⇖⇗⇘⇙⇚⇛⇜⇝⇞⇟⇠⇡⇢⇣⇤⇥⇦⇧⇨⇩⇪⇵∀∂∃∅∉∋∎∏∑≮≯∓∕⁄∗∘∙∝∟∠∣∥∧∫∬∮∴∵∶∷∼∽∾≃≅≌≒≖≣≦≧≪≫≬≳≺≻⊁⊃⊆⊇⊕⊖⊗⊘⊙⊚⊛⊞⊟⊥⊮⊰⊱⋭⊶⊹⊿⋁⋂⋃⋅⋆⋈⋒⋘⋙⋮⋯⋰⋱■□▢▣▤▥▦▧▨▩▬▭▮▰△▴▵▷▸▹►▻▽▾▿◁◂◃◄◅◆◇◈◉◌◍◎◐◑◒◓◔◕◖◗◘◙◜◝◞◟◠◡◢◣◤◥◦◳◷◻◽◿⨧⨯⨼⩣⩽⪍⪚⪺₢₣₤₰₳₶₷₨﷼"},
        // };
        // get the maximum suborder for each subcategory
        // Map<String, Long> subcategoryToMaxSuborder = new HashMap<>();
        // for (String[] row : data) {
        // final String subcategory = row[0];
        // for (Entry<String, String> entry : emojiToMinorCategory.entrySet()) {
        //     if (entry.getValue().equals(subcategory)) {
        //         String emoji = entry.getKey();
        //         Long order = emojiToOrder.get(emoji);
        //         Long currentMax = subcategoryToMaxSuborder.get(subcategory);
        //         if (order == null) continue;
        //         if (currentMax == null || currentMax < order) {
        //             subcategoryToMaxSuborder.put(subcategory, order);
        //         }
        //     }
        // }
        // }
        // if (DEBUG) System.out.println(subcategoryToMaxSuborder);
        // Map<String, Long> _EXTRA_SYMBOL_ORDER = new LinkedHashMap<>();
        // for (String[] row : data) {
        //     final String subcategory = row[0];
        //     final String characters = row[1];
        //
        //     List<String> items = new ArrayList<>();
        //     for (int cp : With.codePointArray(characters)) {
        //         if (cp != ' ') {
        //             items.add(With.fromCodePoint(cp));
        //         }
        //     }
        //     final UnicodeSet uset = new UnicodeSet().addAll(items);
        //     if (uset.containsSome(EXTRA_SYMBOL_MINOR_CATEGORIES.keySet())) {
        //         throw new IllegalArgumentException("Duplicate values in " + EXTRA_SYMBOL_MINOR_CATEGORIES);
        //     }
        //     EXTRA_SYMBOL_MINOR_CATEGORIES.putAll(uset, subcategory);
        //     final Long countObject = subcategoryToMaxSuborder.get(subcategory);
        //     if (countObject == null) continue;
        //     long count = countObject;
        //     for (String s : items) {
        //         ++count;
        //         _EXTRA_SYMBOL_ORDER.put(s, count);
        //     }
        //     subcategoryToMaxSuborder.put(subcategory, count);
        // }
        // if (DEBUG) System.out.println(_EXTRA_SYMBOL_ORDER);
        // EXTRA_SYMBOL_MINOR_CATEGORIES.freeze();
        // EXTRA_SYMBOL_ORDER = ImmutableMap.copyOf(_EXTRA_SYMBOL_ORDER);

        /*
            # group: Smileys & People
            # subgroup: face-positive
            1F600 ; fully-qualified     # 😀 grinning face
         */
        final Splitter semi = Splitter.on(CharMatcher.anyOf(";#")).trimResults();
        String majorCategory;

        final String file = "res/raw/emoji_test.txt";
        final ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader == null) {
            Log.e(TAG, "Emoji: classLoader is null");
            return;
        }
        try (final InputStream in = classLoader.getResourceAsStream(file);
             final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            EmojiCategoryType categoryType = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    line = line.substring(1).trim();
                    if (line.startsWith("group:")) {
                        majorCategory = line.substring("group:".length()).trim();
                        if (!majorCategory.equals("Component")) { // Skip Component
                            if (majorCategory.equals("Smileys & Emotion") || majorCategory.equals("People & Body")) {
                                // Put 'People & Body' in 'Smileys & Emotion' category
                                categoryType = EmojiCategoryType.SMILEYS_AND_EMOTION;
                            } else {
                                categoryType = EmojiCategoryType.valueOfName(majorCategory);
                            }
                            final boolean contains = CATEGORY_MAP.containsKey(categoryType);
                            if (!contains) {
                                CATEGORY_MAP.put(categoryType, new EmojiCategory(categoryType));
                            }
                        }
                    }
                    continue;
                }
                if (categoryType == null) continue;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                final Iterator<String> it = semi.split(line).iterator();
                String emojiHex = it.next();
                String original = Utility.fromHex(emojiHex, 4, " ");
                String status = it.next();
                if (!status.startsWith("fully-qualified")) { // only use fully qualified
                    continue;
                }
                final EmojiCategory emojiCategory = CATEGORY_MAP.get(categoryType);
                final Map<String, Emoji> emojis = emojiCategory == null ? new LinkedHashMap<>() : emojiCategory.getEmojis();
                String comment = it.next();
                // The comment is now of the form:  # 😁 E0.6 beaming face with smiling eyes
                int spacePos = comment.indexOf(' ');
                spacePos = comment.indexOf(' ', spacePos + 1); // get second space
                final String name = comment.substring(spacePos + 1).trim();
                final Emoji emoji = new Emoji(original, name);
                ALL_EMOJIS.put(original, emoji);
                String minimal = original.replace(EMOJI_VARIANT, "");
                //noinspection deprecation
                boolean singleton = CharSequences.getSingleCodePoint(minimal) != Integer.MAX_VALUE;
                if (!singleton && SKIN_TONE_MODIFIERS.containsSome(minimal)) {
                    // skin tone variant
                    final String parent = minimal.replaceAll(SKIN_TONE_PATTERN, "");
                    final Emoji parentEmoji = emojis.get(parent);
                    if (parentEmoji != null) {
                        parentEmoji.addVariant(emoji);
                    }
                    continue;
                }
                emojis.put(original, emoji);
                // skip constructed values
                // if (minimal.contains(COMBINING_ENCLOSING_KEYCAP)
                //         || REGIONAL_INDICATORS.containsSome(minimal)
                //         || TAGS.containsSome(minimal)
                //         || !singleton && MODIFIERS.containsSome(minimal)
                //         || !singleton && FAMILY.containsAll(minimal)) {
                //     // do nothing
                // } else if (minimal.contains(ZWJ)) { // only do certain ZWJ sequences
                //     if (SPECIALS.contains(minimal)
                //             || GENDER.containsSome(minimal)
                //             || MAN_WOMAN.contains(minimal.codePointAt(0)) && OBJECT.contains(minimal.codePointBefore(minimal.length()))) {
                //         // nonConstructed.add(minimal);
                //     }
                // } else if (!minimal.contains("🔟")) {
                //     // nonConstructed.add(minimal);
                // }
            }
            //        for (Entry<Pair<Integer,Integer>, String> entry : majorPlusMinorToEmoji.entries()) {
            //            String minimal = entry.getValue();
            //            emojiToOrder.put(minimal, emojiToOrder.size());
            //        }
        } catch (IOException e) {
            Log.e(TAG, "Emoji: ", e);
        }
    }

    // private static <K, V> void putUnique(Map<K, V> map, K key, V value) {
    //     V oldValue = map.put(key, value);
    //     if (oldValue != null) {
    //         throw new ICUException("Attempt to change value of " + map
    //                                        + " for " + key
    //                                        + " from " + oldValue
    //                                        + " to " + value
    //         );
    //     }
    // }

    public Map<EmojiCategoryType, EmojiCategory> getCategoryMap() {
        return CATEGORY_MAP;
    }

    public List<EmojiCategory> getEmojiCategories() {
        if (categories == null) {
            final Collection<EmojiCategory> categoryCollection = CATEGORY_MAP.values();
            categories = ImmutableList.copyOf(categoryCollection);
        }
        return categories;
    }

    public Map<String, Emoji> getAllEmojis() {
        return ALL_EMOJIS;
    }

    // public String getMinorCategory(String emoji) {
    //     String minorCat = emojiToMinorCategory.get(emoji);
    //     if (minorCat == null) {
    //         minorCat = EXTRA_SYMBOL_MINOR_CATEGORIES.get(emoji);
    //         if (minorCat == null) {
    //             throw new InternalCldrException("No minor category (aka subgroup) found for " + emoji
    //                                                     + ". Update emoji-test.txt to latest, and setValue PathHeader.. functionMap.put(\"minor\", ...");
    //         }
    //     }
    //     return minorCat;
    // }

    // public long getEmojiToOrder(String emoji) {
    //     Long result = emojiToOrder.get(emoji);
    //     if (result == null) {
    //         result = EXTRA_SYMBOL_ORDER.get(emoji);
    //         if (result == null) {
    //             throw new InternalCldrException("No Order found for " + emoji
    //                                                     + ". Update emoji-test.txt to latest, and setValue PathHeader.. functionMap.put(\"minor\", ...");
    //         }
    //     }
    //     return result;
    // }

    // public long getEmojiMinorOrder(String minor) {
    //     Long result = minorToOrder.get(minor);
    //     if (result == null) {
    //         throw new InternalCldrException("No minor category (aka subgroup) found for " + minor
    //                                                 + ". Update emoji-test.txt to latest, and setValue PathHeader.. functionMap.put(\"minor\", ...");
    //     }
    //     return result;
    // }

    // public String getMajorCategory(String emoji) {
    //     String majorCat = emojiToMajorCategory.get(emoji);
    //     if (majorCat == null) {
    //         if (EXTRA_SYMBOL_MINOR_CATEGORIES.containsKey(emoji)) {
    //             majorCat = "Symbols";
    //         } else {
    //             throw new InternalCldrException("No minor category (aka subgroup) found for " + emoji
    //                                                     + ". Update emoji-test.txt to latest, and setValue PathHeader.. functionMap.put(\"major\", ...");
    //         }
    //     }
    //     return majorCat;
    // }

    // public Set<String> getMinorCategoriesWithExtras() {
    //     Set<String> result = new LinkedHashSet<>(emojiToMinorCategory.values());
    //     result.addAll(EXTRA_SYMBOL_MINOR_CATEGORIES.getAvailableValues());
    //     return ImmutableSet.copyOf(result);
    // }

    // public UnicodeSet getEmojiInMinorCategoriesWithExtras(String minorCategory) {
    //     return new UnicodeSet(emojiToMinorCategory.getSet(minorCategory))
    //             .addAll(EXTRA_SYMBOL_MINOR_CATEGORIES.getSet(minorCategory))
    //             .freeze();
    // }

    // public synchronized Set<String> getNamePaths() {
    //     return NAME_PATHS != null ? NAME_PATHS : (NAME_PATHS = buildPaths(TYPE_TTS));
    // }

    // public synchronized Set<String> getKeywordPaths() {
    //     return KEYWORD_PATHS != null ? KEYWORD_PATHS : (KEYWORD_PATHS = buildPaths(""));
    // }

    // private ImmutableSet<String> buildPaths(String suffix) {
    //     ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    //     for (String s : getNonConstructed()) {
    //         String base = "//ldml/annotations/annotation[@cp=\"" + s + "\"]" + suffix;
    //         builder.add(base);
    //     }
    //     return builder.build();
    // }
}

