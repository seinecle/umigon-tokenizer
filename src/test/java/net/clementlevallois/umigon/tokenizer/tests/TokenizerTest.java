/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.umigon.tokenizer.tests;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.clementlevallois.umigon.model.Emoji;
import net.clementlevallois.umigon.model.TextFragment;
import net.clementlevallois.umigon.model.TypeOfTextFragment.TypeOfTextFragmentEnum;
import net.clementlevallois.umigon.tokenizer.controller.UmigonTokenizer;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author LEVALLOIS
 */
public class TokenizerTest {

    @Test
    public void proceedToTests() {
        String text1 = "provides a fine-grained analysis";
        String text2 = "I love chocolate";
        String text3 = " I can't *wait*  to see this performance! 𝄠\nI will l@@@ve it :-) 😀😀😀 😀 :((( ";
        String text4 = "I love chocolate :-), really (esp5ecially with coffee!)";
        String text5 = "This app is amazing";
        String text6 = "nocode is the new thing :) 🤔";
        String text7 = "this is ‘Beautiful And Sad At The Same Time’, right.";
        String text8 = "😊😊😊😊😊😊";
        List<String> texts = List.of(text1, text2, text3, text4, text5, text6, text7, text8);
        int i = 1;
        for (String text : texts) {
//            System.out.println("text: " + text);
//            System.out.println("");
            Set<String> languageSpecificLexicon = new HashSet();
            List<TextFragment> textFragments = UmigonTokenizer.tokenize(text, languageSpecificLexicon);
            String beautiffiedPrint = printTextFragments(textFragments);
            if (i == 1) {
                Assert.assertEquals(9, textFragments.size());
            }
            if (i == 2) {
                Assert.assertEquals(5, textFragments.size());
            }
            if (i == 3) {
                Assert.assertEquals("can't", textFragments.get(3).getOriginalForm());
            }
            if (i == 6) {
                Assert.assertEquals(TypeOfTextFragmentEnum.EMOTICON_IN_ASCII, textFragments.get(10).getTypeOfTextFragmentEnum());
                Assert.assertEquals(TypeOfTextFragmentEnum.EMOJI, textFragments.get(12).getTypeOfTextFragmentEnum());
            }
            if (i == 7) {
                Assert.assertEquals(23, textFragments.size());
            }
            if (i == 8) {
                Emoji tf = (Emoji)textFragments.get(0);
                Assert.assertEquals(tf.getOriginalForm(), "😊");
                Assert.assertEquals(tf.getSemiColonForm(), ":blush:");
            }
            i++;
        }
    }

    private String printTextFragments(List<TextFragment> textFragments) {
        StringBuilder sb = new StringBuilder();
        for (TextFragment text : textFragments) {
            sb.append("text fragment: ").append(text.getOriginalForm());
            sb.append(" (type: ").append(text.getTypeOfTextFragmentEnum()).append(")");
            sb.append("\n");
        }
        return sb.toString();
    }

}
