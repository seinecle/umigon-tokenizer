/*
 * author: Clément Levallois
 */
package net.clementlevallois.umigon.tokenizer.controller;

import com.vdurmont.emoji.EmojiParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.clementlevallois.umigon.model.NonWord;
import net.clementlevallois.umigon.model.PatternOfInterest;
import net.clementlevallois.umigon.model.Term;
import net.clementlevallois.umigon.model.TextFragment;
import net.clementlevallois.umigon.model.TypeOfTextFragment;
import net.clementlevallois.umigon.model.WhiteSpace;
import net.clementlevallois.utils.RepeatedCharactersRemover;
import net.clementlevallois.utils.TextCleaningOps;

/**
 *
 * @author LEVALLOIS
 */
public class UmigonTokenizer {

    public static void main(String[] args) throws IOException {
//        String text = "J'aime la \"vie\" #wow what a performance! 𝄠\nI l@@@ve it :-) 😀😀😀 😀 :((( http://allo";
        String text = "Il a de vraies burnes";
        System.out.println("text: " + text);
        Set<String> languageSpecificLexicon = new HashSet();
        UmigonTokenizer controller = new UmigonTokenizer();
        List<TextFragment> textFragments = controller.tokenize(text, languageSpecificLexicon);
        String beautiffiedPrint = controller.printTextFragments(textFragments);
        System.out.println(beautiffiedPrint);
    }

    private enum CurrentFragment {
        CURR_FRAGMENT_IS_WHITE_SPACE, CURR_FRAGMENT_IS_PUNCT, CURR_FRAGMENT_IS_TERM, CURR_FRAGMENT_IS_NOT_STARTED, CURR_FRAGMENT_IS_EMOJI
    }

    public static List<TextFragment> tokenize(String text, Set<String> languageSpecificLexicon) throws IOException {
        PatternOfInterestChecker.loadPatternsOfInterest();
        List<TextFragment> textFragments = new ArrayList();

        boolean textFragmentStarted = false;
        boolean isCurrCodePointWhiteSpace = false;
        boolean isCurrCodePointTerm = false;
        boolean isCurrCodePointEmoji = false;
        boolean isCurrCodPointPunctuation = false;
        CurrentFragment currFragment = CurrentFragment.CURR_FRAGMENT_IS_NOT_STARTED;
        WhiteSpace whiteSpaceFragment = null;
        Term term = null;
        int i = 0;

        int[] codePoints = text.codePoints().toArray();

        System.out.println("length of the text: " + text.length());
        System.out.println("number of code points: " + codePoints.length);

        for (int codePoint : codePoints) {
            String stringOfCodePoint = Character.toString(codePoint);
//            if (stringOfCodePoint.equals("s")) {
//                System.out.println("stop there is a s");
//            }

            //check if this is a punctuation mark
            isCurrCodPointPunctuation = Pattern.matches("[\\p{Punct}\\p{IsPunctuation}]", stringOfCodePoint);

            // check if this is an emoji
            List<String> extractEmojis = EmojiParser.extractEmojis(stringOfCodePoint);
            isCurrCodePointEmoji = !extractEmojis.isEmpty();

            // check if this is a whitespace
            isCurrCodePointWhiteSpace = stringOfCodePoint.isBlank();

            switch (currFragment) {
                case CURR_FRAGMENT_IS_WHITE_SPACE:
                    if (isCurrCodePointWhiteSpace) {
                        whiteSpaceFragment.addStringToString(stringOfCodePoint);
                        if (stringOfCodePoint.equals("\n")) {
                            whiteSpaceFragment.setSentenceOrLineBreak(Boolean.TRUE);
                        }
                    } else if (!isCurrCodePointEmoji) {
                        textFragments.add(whiteSpaceFragment);
                        textFragmentStarted = false;
                    } else {
                        textFragments.add(whiteSpaceFragment);
                        textFragmentStarted = false;
                        term = new Term();
                        currFragment = CurrentFragment.CURR_FRAGMENT_IS_EMOJI;
                        term.setTypeOfTextFragment(TypeOfTextFragment.TypeOfTextFragmentEnum.EMOJI);
                        term.setIndexCardinal(i);
                        term.setIndexOrdinal(textFragments.size());
                        term.addStringToString(stringOfCodePoint);
                        textFragments.add(term);
                    }
                    break;

                case CURR_FRAGMENT_IS_TERM:
                    if ((isCurrCodePointWhiteSpace) | (isCurrCodPointPunctuation && !term.getString().startsWith("http"))) {
                        String originalForm = term.getString();
                        term.setOriginalForm(originalForm);
                        String cleanedForm = RepeatedCharactersRemover.repeatedCharacters(originalForm, languageSpecificLexicon);
                        String cleanedAndStrippedForm = TextCleaningOps.flattenToAscii(cleanedForm);
                        term.setCleanedForm(cleanedForm);
                        term.setCleanedAndStrippedForm(cleanedAndStrippedForm);

                        // some term fragments can be onomatopaes (wooow) or texto speak (lol, rofl)
                        // we call them 'non words' (for lack of better name)
                        // they must be detected to be attributed a specific type (different from "term"):
                        PatternOfInterest returnsMatchOrNot = PatternOfInterestChecker.returnsMatchOrNot(term.getCleanedAndStrippedForm());
                        if (returnsMatchOrNot.getMatched()) {
                            NonWord nonWord = new NonWord();
                            nonWord.setIndexCardinal(term.getIndexCardinal());
                            nonWord.setIndexOrdinal(term.getIndexOrdinal());
                            nonWord.setString(term.getString());
                            nonWord.setTypeOfTextFragment(returnsMatchOrNot.getTypeOfTextFragment());
                            nonWord.setPoi(returnsMatchOrNot);
                            textFragments.add(nonWord);
                        } else {
                            textFragments.add(term);
                        }
                        textFragmentStarted = false;
                    } else if (!isCurrCodePointEmoji) {
                        term.addStringToString(stringOfCodePoint);
                    } else if (isCurrCodePointEmoji) {
                        textFragments.add(term);
                        textFragmentStarted = false;
                        term = new Term();
                        term.setTypeOfTextFragment(TypeOfTextFragment.TypeOfTextFragmentEnum.EMOJI);
                        currFragment = CurrentFragment.CURR_FRAGMENT_IS_EMOJI;
                        term.setIndexCardinal(i);
                        term.setIndexOrdinal(textFragments.size());
                        term.addStringToString(stringOfCodePoint);
                        textFragments.add(term);
                    }
                    break;

                case CURR_FRAGMENT_IS_PUNCT:
                    if (!isCurrCodPointPunctuation) {
                        if (term.getString().codePoints().toArray().length > 1) {
                            PatternOfInterest returnsMatchOrNot = PatternOfInterestChecker.returnsMatchOrNot(term.getString());
                            if (returnsMatchOrNot.getMatched()) {
                                term.setTypeOfTextFragment(returnsMatchOrNot.getTypeOfTextFragment());
                                textFragments.add(term);
                                textFragmentStarted = false;
                            } else {
                                int[] codePointsPunct = term.getString().codePoints().toArray();
                                for (int codePointPunct : codePointsPunct) {
                                    String punct = Character.toString(codePointPunct);
                                    term = new Term();
                                    term.setTypeOfTextFragment(TypeOfTextFragment.TypeOfTextFragmentEnum.PUNCTUATION);
                                    currFragment = CurrentFragment.CURR_FRAGMENT_IS_PUNCT;
                                    term.setIndexCardinal(i);
                                    term.setIndexOrdinal(textFragments.size());
                                    term.addStringToString(punct);
                                    textFragments.add(term);
                                }
                                textFragmentStarted = false;
                            }
                        } else {
                            textFragments.add(term);
                            textFragmentStarted = false;
                        }
                    } else {
                        term.addStringToString(stringOfCodePoint);
                    }
                    break;

                case CURR_FRAGMENT_IS_EMOJI:
                    if (isCurrCodePointEmoji) {
                        term = new Term();
                        currFragment = CurrentFragment.CURR_FRAGMENT_IS_EMOJI;
                        term.setIndexCardinal(i);
                        term.setIndexOrdinal(textFragments.size());
                        term.addStringToString(stringOfCodePoint);
                        term.setTypeOfTextFragment(TypeOfTextFragment.TypeOfTextFragmentEnum.EMOJI);
                        textFragments.add(term);
                        textFragmentStarted = false;
                    }
                    break;

            }

            if (!textFragmentStarted) {
                if (isCurrCodePointWhiteSpace) {
                    textFragmentStarted = true;
                    whiteSpaceFragment = new WhiteSpace();
                    whiteSpaceFragment.setTypeOfTextFragment(TypeOfTextFragment.TypeOfTextFragmentEnum.WHITE_SPACE);
                    whiteSpaceFragment.setIndexCardinal(i);
                    whiteSpaceFragment.setIndexOrdinal(textFragments.size());
                    currFragment = CurrentFragment.CURR_FRAGMENT_IS_WHITE_SPACE;
                    whiteSpaceFragment.addStringToString(stringOfCodePoint);
                    if (stringOfCodePoint.equals("\n")) {
                        whiteSpaceFragment.setSentenceOrLineBreak(Boolean.TRUE);
                    }
                } else if (!isCurrCodePointEmoji & !isCurrCodPointPunctuation) {
                    textFragmentStarted = true;
                    term = new Term();
                    term.setTypeOfTextFragment(TypeOfTextFragment.TypeOfTextFragmentEnum.TERM);
                    currFragment = CurrentFragment.CURR_FRAGMENT_IS_TERM;
                    term.setIndexCardinal(i);
                    term.setIndexOrdinal(textFragments.size());
                    term.addStringToString(stringOfCodePoint);
                } else if (isCurrCodPointPunctuation) {
                    textFragmentStarted = true;
                    term = new Term();
                    term.setTypeOfTextFragment(TypeOfTextFragment.TypeOfTextFragmentEnum.PUNCTUATION);
                    currFragment = CurrentFragment.CURR_FRAGMENT_IS_PUNCT;
                    term.setIndexCardinal(i);
                    term.setIndexOrdinal(textFragments.size());
                    term.addStringToString(stringOfCodePoint);
                }
            }
            i++;
            if (i == codePoints.length) {
                if (isCurrCodePointWhiteSpace & whiteSpaceFragment != null) {
                    textFragments.add(whiteSpaceFragment);
                }
                if (!isCurrCodePointWhiteSpace & !isCurrCodePointEmoji & term != null) {
                    if (!isCurrCodPointPunctuation) {
                        String originalForm = term.getString();
                        term.setOriginalForm(originalForm);
                        String cleanedForm = RepeatedCharactersRemover.repeatedCharacters(originalForm, languageSpecificLexicon);
                        String cleanedAndStrippedForm = TextCleaningOps.flattenToAscii(cleanedForm);
                        term.setCleanedForm(cleanedForm);
                        term.setCleanedAndStrippedForm(cleanedAndStrippedForm);
                        textFragments.add(term);
                    } else {
                        if (term.getString().codePoints().toArray().length > 1) {
                            PatternOfInterest returnsMatchOrNot = PatternOfInterestChecker.returnsMatchOrNot(term.getString());
                            if (returnsMatchOrNot.getMatched()) {
                                NonWord nonWord = new NonWord();
                                nonWord.setIndexCardinal(term.getIndexCardinal());
                                nonWord.setIndexOrdinal(term.getIndexOrdinal());
                                nonWord.setString(term.getString());
                                nonWord.setTypeOfTextFragment(returnsMatchOrNot.getTypeOfTextFragment());
                                nonWord.setPoi(returnsMatchOrNot);
                                textFragments.add(nonWord);
                            } else {
                                int[] codePointsPunct = term.getString().codePoints().toArray();
                                for (int codePointPunct : codePointsPunct) {
                                    String punct = Character.toString(codePointPunct);
                                    term = new Term();
                                    currFragment = CurrentFragment.CURR_FRAGMENT_IS_PUNCT;
                                    term.setIndexCardinal(i);
                                    term.setIndexOrdinal(textFragments.size());
                                    term.addStringToString(punct);
                                    term.setTypeOfTextFragment(TypeOfTextFragment.TypeOfTextFragmentEnum.PUNCTUATION);
                                    textFragments.add(term);
                                }
                            }
                        } else {
                            textFragments.add(term);
                        }

                    }
                }
            }
        }

        return textFragments;

    }

    private String printTextFragments(List<TextFragment> textFragments) {
        StringBuilder sb = new StringBuilder();
        for (TextFragment text : textFragments) {
            sb.append("text fragment: ").append(text.getString());
            sb.append("\n");
        }
        return sb.toString();
    }

}
