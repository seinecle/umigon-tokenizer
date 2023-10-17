/*
 * author: Clément Levallois
 */
package net.clementlevallois.umigon.tokenizer.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import net.clementlevallois.umigon.model.Emoji;
import net.clementlevallois.umigon.model.NonWord;
import net.clementlevallois.umigon.model.PatternOfInterest;
import net.clementlevallois.umigon.model.Punctuation;
import net.clementlevallois.umigon.model.Term;
import net.clementlevallois.umigon.model.TextFragment;
import net.clementlevallois.umigon.model.WhiteSpace;
import net.clementlevallois.utils.RepeatedCharactersRemover;
import net.clementlevallois.utils.TextCleaningOps;
import net.fellbaum.jemoji.EmojiManager;

/**
 *
 * @author LEVALLOIS
 */
public class UmigonTokenizer {

    static boolean initialized = false;
    static private PatternOfInterestChecker poiChecker;

    private enum CurrentFragment {
        CURR_FRAGMENT_IS_WHITE_SPACE, CURR_FRAGMENT_IS_PUNCTUATION, CURR_FRAGMENT_IS_NON_WORD, CURR_FRAGMENT_IS_TERM, CURR_FRAGMENT_IS_NOT_STARTED
    }

    public static void initialize() {
        poiChecker = new PatternOfInterestChecker();
        poiChecker.loadPatternsOfInterest();
        initialized = true;

    }

    public static List<TextFragment> tokenize(String text, Set<String> languageSpecificLexicon) {
        if (!initialized) {
            poiChecker = new PatternOfInterestChecker();
            poiChecker.loadPatternsOfInterest();
        }
        List<TextFragment> textFragments = new ArrayList();
        if (languageSpecificLexicon == null) {
            languageSpecificLexicon = new HashSet();
        }

        boolean textFragmentStarted = false;
        boolean isCurrCodePointWhiteSpace = false;
        boolean isCurrCodePointTerm = false;
        boolean isCurrCodePointEmoji = false;
        boolean isCurrCodPointPunctuation = false;

        // from https://www.compart.com/en/unicode/category/Pd
        Set<String> dashLikeCharacters = Set.of("-", "‐", "‑", "‒", "–", "—", "︱", "﹘", "﹣", "－", "_", "\\", "/", "|");

        boolean dontStartNewFragment = false;
        CurrentFragment currFragment = CurrentFragment.CURR_FRAGMENT_IS_NOT_STARTED;

        WhiteSpace whiteSpace = null;
        Term term = null;
        Punctuation punctuation = null;
        NonWord nonWord = null;
        Emoji emoji = null;

        int[] codePoints = text.codePoints().toArray();

        for (int indexCurrentCodePoint = 0; indexCurrentCodePoint < codePoints.length; indexCurrentCodePoint++) {

            int currentCodePoint = codePoints[indexCurrentCodePoint];
            String stringOfCodePoint = Character.toString(currentCodePoint);

//            if (stringOfCodePoint.equals(",")) {
//                System.out.println("stop there is a ,");
//            }

            /* if we have started a text fragment of the type "term":
                - we want to check whether the last character before the next whote space is a letter
                - if so, down below the logic of the tokenizer will leverage this info to accept punctuation signs in the text fragment
                - BUT NOT hyphens and similar chars
                - as in: "l@@@@ve" will be accepted as one text fragment
                - but "reached)" will be decomposed in "reached" and ")"
             */
            int nextCodePoint = -99;
            boolean isCodePointBeforeNextWhiteSpaceALetter = false;
            if (currFragment == CurrentFragment.CURR_FRAGMENT_IS_TERM) {
                // if there is a codepoint after this one, record it in a variable
                int beforeWhiteSpaceCodePoint = -99;
                for (int j = (indexCurrentCodePoint + 1); j < codePoints.length; j++) {
                    beforeWhiteSpaceCodePoint = codePoints[j - 1];
                    int currCodepointInSearchOfWhiteSpace = codePoints[j];
                    boolean isWhiteSpaceFound = Character.isWhitespace(currCodepointInSearchOfWhiteSpace);
                    if (isWhiteSpaceFound) {
                        if (Character.isLetter(beforeWhiteSpaceCodePoint)) {
                            isCodePointBeforeNextWhiteSpaceALetter = true;
                        }
                        break;
                    }
                }
            }
            dontStartNewFragment = false;

            //check if this is a punctuation mark
            isCurrCodPointPunctuation = Pattern.matches("[\\p{Punct}\\p{IsPunctuation}]", stringOfCodePoint);

            // check if this is an emoji
            isCurrCodePointEmoji = EmojiManager.isEmoji(stringOfCodePoint);

            // check if this is a whitespace
            isCurrCodePointWhiteSpace = stringOfCodePoint.isBlank();

            switch (currFragment) {
                case CURR_FRAGMENT_IS_WHITE_SPACE:
                    if (isCurrCodePointWhiteSpace) {
                        whiteSpace.addStringToOriginalForm(stringOfCodePoint);
                        if (stringOfCodePoint.equals("\n")) {
                            whiteSpace.setSentenceOrLineBreak(Boolean.TRUE);
                        }
                    } else {
                        textFragments.add(whiteSpace);
                        textFragmentStarted = false;
                    }
                    break;

                case CURR_FRAGMENT_IS_TERM:
                    String originalForm = term.getOriginalForm();

                    if (isCurrCodePointWhiteSpace) {
                        String cleanedForm = RepeatedCharactersRemover.repeatedCharacters(originalForm, languageSpecificLexicon);
                        String cleanedAndStrippedForm = TextCleaningOps.flattenToAscii(cleanedForm);
                        term.setCleanedForm(cleanedForm);
                        term.setCleanedAndStrippedForm(cleanedAndStrippedForm);

                        // some term fragments can be onomatopaes (wooow) or texto speak (lol, rofl)
                        // we call them 'non words' (for lack of better name)
                        // they must be detected to be attributed a specific type (different from "term"):
                        PatternOfInterest returnsMatchOrNot = poiChecker.returnsMatchOrNot(term.getCleanedAndStrippedForm());
                        if (returnsMatchOrNot.getMatched()) {
                            nonWord = new NonWord();
                            nonWord.setIndexCardinal(term.getIndexCardinal());
                            nonWord.setIndexOrdinal(term.getIndexOrdinal());
                            nonWord.setOriginalForm(term.getOriginalForm());
                            nonWord.setTypeOfTextFragmentEnum(returnsMatchOrNot.getTypeOfTextFragmentEnum());
                            nonWord.setPoi(returnsMatchOrNot);
                            textFragments.add(nonWord);
                        } else {
                            textFragments.add(term);
                        }
                        textFragmentStarted = false;
                    } else // the following condition should capture code points which are plain characters...
                    if (!isCurrCodePointEmoji & !isCurrCodPointPunctuation) {
                        term.addStringToOriginalForm(stringOfCodePoint);
                    } else if (isCurrCodePointEmoji) {
                        String cleanedForm = RepeatedCharactersRemover.repeatedCharacters(originalForm, languageSpecificLexicon);
                        String cleanedAndStrippedForm = TextCleaningOps.flattenToAscii(cleanedForm);
                        term.setCleanedForm(cleanedForm);
                        term.setCleanedAndStrippedForm(cleanedAndStrippedForm);
                        textFragments.add(term);
                        textFragmentStarted = false;
                    } else /*
                        what if we are in a term fragment and the current point is a punctuation sign?
                        - either the last character before the next whitespace will be alphabetical,
                            -> in which case, *if the punctuation sign is NOT an hyphen NOR a dash*, we add the current punctuation sign to the term
                        - or it is not, in which case we close the term and start a new text fragment
                     */ if (isCurrCodPointPunctuation && isCodePointBeforeNextWhiteSpaceALetter && !dashLikeCharacters.contains(stringOfCodePoint)) {
                        term.addStringToOriginalForm(stringOfCodePoint);
                    } else {
                        String cleanedForm = RepeatedCharactersRemover.repeatedCharacters(originalForm, languageSpecificLexicon);
                        String cleanedAndStrippedForm = TextCleaningOps.flattenToAscii(cleanedForm);
                        term.setCleanedForm(cleanedForm);
                        term.setCleanedAndStrippedForm(cleanedAndStrippedForm);
                        textFragments.add(term);
                        textFragmentStarted = false;
                    }
                    break;

                case CURR_FRAGMENT_IS_NON_WORD:
                    if ((isCurrCodePointWhiteSpace || isCurrCodePointEmoji)) {
                        textFragments.add(nonWord);
                        textFragmentStarted = false;
                        nonWord = new NonWord();
                    } else {
                        String currNonWordWithNewChar = nonWord.getOriginalForm() + stringOfCodePoint;
                        PatternOfInterest returnsMatchOrNot = poiChecker.returnsMatchOrNot(currNonWordWithNewChar);
                        if (returnsMatchOrNot.getMatched()) {
                            nonWord.setOriginalForm(currNonWordWithNewChar);
                            nonWord.setPoi(returnsMatchOrNot);
                            currFragment = CurrentFragment.CURR_FRAGMENT_IS_NON_WORD;
                        } else {
                            textFragments.add(nonWord);
                            textFragmentStarted = false;
                            dontStartNewFragment = false;
                            nonWord = new NonWord();
                        }
                    }
                    break;

                case CURR_FRAGMENT_IS_PUNCTUATION:
                    PatternOfInterest poi = poiChecker.returnsMatchOrNot(punctuation.getOriginalForm());
                    if (poi.getMatched()) {
                        nonWord = punctuation.toNonWord(poi, punctuation.getOriginalForm());
                        textFragments.add(nonWord);
                        textFragmentStarted = false;
                        break;
                    }
                    if (isCurrCodPointPunctuation) {
                        String currPunctWithNewChar = punctuation.getOriginalForm() + stringOfCodePoint;
                        if (currPunctWithNewChar.codePoints().toArray().length > 1) {
                            poi = poiChecker.returnsMatchOrNot(currPunctWithNewChar);
                            if (poi.getMatched()) {
                                nonWord = punctuation.toNonWord(poi, currPunctWithNewChar);
                                currFragment = CurrentFragment.CURR_FRAGMENT_IS_NON_WORD;
                                break;
                            } else {
                                punctuation.addStringToOriginalForm(stringOfCodePoint);
                            }
                        }
                    } else {
                        int[] codePointsPunct = punctuation.getOriginalForm().codePoints().toArray();
                        currFragment = CurrentFragment.CURR_FRAGMENT_IS_PUNCTUATION;
                        for (int codePointPunct : codePointsPunct) {
                            String punct = Character.toString(codePointPunct);
                            punctuation = new Punctuation();
                            punctuation.setIndexCardinal(indexCurrentCodePoint);
                            punctuation.setIndexOrdinal(textFragments.size());
                            punctuation.addStringToOriginalForm(punct);
                            textFragments.add(punctuation);
                        }
                        textFragmentStarted = false;
                    }
            }

            if (!textFragmentStarted & !dontStartNewFragment) {
                if (isCurrCodePointWhiteSpace) {
                    textFragmentStarted = true;
                    whiteSpace = new WhiteSpace();
                    whiteSpace.setIndexCardinal(indexCurrentCodePoint);
                    whiteSpace.setIndexOrdinal(textFragments.size());
                    currFragment = CurrentFragment.CURR_FRAGMENT_IS_WHITE_SPACE;
                    whiteSpace.addStringToOriginalForm(stringOfCodePoint);
                    if (stringOfCodePoint.equals("\n")) {
                        whiteSpace.setSentenceOrLineBreak(Boolean.TRUE);
                    }
                } else if (!isCurrCodePointEmoji & !isCurrCodPointPunctuation) {
                    textFragmentStarted = true;
                    term = new Term();
                    currFragment = CurrentFragment.CURR_FRAGMENT_IS_TERM;
                    term.setIndexCardinal(indexCurrentCodePoint);
                    term.setIndexOrdinal(textFragments.size());
                    term.addStringToOriginalForm(stringOfCodePoint);
                } else if (isCurrCodPointPunctuation) {
                    textFragmentStarted = true;
                    punctuation = new Punctuation();
                    currFragment = CurrentFragment.CURR_FRAGMENT_IS_PUNCTUATION;
                    punctuation.setIndexCardinal(indexCurrentCodePoint);
                    punctuation.setIndexOrdinal(textFragments.size());
                    punctuation.addStringToOriginalForm(stringOfCodePoint);
                } else if (isCurrCodePointEmoji) {
                    emoji = new Emoji();
                    emoji.setIndexCardinal(indexCurrentCodePoint);
                    emoji.setIndexOrdinal(textFragments.size());
                    emoji.addStringToOriginalForm(stringOfCodePoint);
                    Optional<net.fellbaum.jemoji.Emoji> emojiObject = EmojiManager.getEmoji(stringOfCodePoint);
                    if (emojiObject.isPresent()) {
                        List<String> aliases = emojiObject.get().getGithubAliases();
                        if (!aliases.isEmpty()) {
                            emoji.setSemiColonForm(aliases.get(0));
                        } else {
                            aliases = emojiObject.get().getSlackAliases();
                            if (!aliases.isEmpty()) {
                                emoji.setSemiColonForm(aliases.get(0));
                            }
                        }
                    }
                    textFragments.add(emoji);
                    textFragmentStarted = false;
                    currFragment = CurrentFragment.CURR_FRAGMENT_IS_NOT_STARTED;
                }
            }
            if ((indexCurrentCodePoint + 1) == codePoints.length) {

                if (currFragment == CurrentFragment.CURR_FRAGMENT_IS_WHITE_SPACE) {
                    textFragments.add(whiteSpace);
                }

                if (currFragment == CurrentFragment.CURR_FRAGMENT_IS_NON_WORD) {
                    textFragments.add(nonWord);
                }

                if (currFragment == CurrentFragment.CURR_FRAGMENT_IS_TERM) {
                    String originalForm = term.getOriginalForm();
                    String cleanedForm = RepeatedCharactersRemover.repeatedCharacters(originalForm, languageSpecificLexicon);
                    String cleanedAndStrippedForm = TextCleaningOps.flattenToAscii(cleanedForm);
                    term.setCleanedForm(cleanedForm);
                    term.setCleanedAndStrippedForm(cleanedAndStrippedForm);
                    textFragments.add(term);
                }

                if (currFragment == CurrentFragment.CURR_FRAGMENT_IS_PUNCTUATION) {
                    PatternOfInterest returnsMatchOrNot = poiChecker.returnsMatchOrNot(punctuation.getOriginalForm());
                    if (returnsMatchOrNot.getMatched()) {
                        nonWord = new NonWord();
                        nonWord.setIndexCardinal(punctuation.getIndexCardinal());
                        nonWord.setIndexOrdinal(punctuation.getIndexOrdinal());
                        nonWord.setOriginalForm(punctuation.getOriginalForm());
                        nonWord.setTypeOfTextFragmentEnum(returnsMatchOrNot.getTypeOfTextFragmentEnum());
                        nonWord.setPoi(returnsMatchOrNot);
                        textFragments.add(nonWord);
                    } else {
                        int[] codePointsPunct = punctuation.getOriginalForm().codePoints().toArray();
                        for (int codePointPunct : codePointsPunct) {
                            String punct = Character.toString(codePointPunct);
                            punctuation = new Punctuation();
                            punctuation.setIndexCardinal(indexCurrentCodePoint);
                            punctuation.setIndexOrdinal(textFragments.size());
                            punctuation.addStringToOriginalForm(punct);
                            textFragments.add(punctuation);
                        }
                    }

                }
            }
        }

        return textFragments;

    }

}
