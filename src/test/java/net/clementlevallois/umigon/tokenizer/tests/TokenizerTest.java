/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.umigon.tokenizer.tests;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.clementlevallois.umigon.model.TextFragment;
import net.clementlevallois.umigon.tokenizer.controller.UmigonTokenizer;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author LEVALLOIS
 */
public class TokenizerTest {

    @Test
    public void proceedToTests() throws IOException {
        String text1 = "provides a fine-grained analysis";
        String text2 = "I love chocolate";
        String text3 = " I can't *wait*  to see this performance! 𝄠\nI will l@@@ve it :-) 😀😀😀 😀 :((( ";
        String text4 = "I love chocolate :-), really (esp5ecially with coffee!)";
        String text5 = "This app is amazing";
        String text6 = "nocode is the new thing :) 🤔";
        String text7 = "this is ‘Beautiful And Sad At The Same Time’, right.";
        List<String> texts = List.of(text1, text2, text3, text4, text5, text6, text7);
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
            if (i == 7) {
                Assert.assertEquals(23, textFragments.size());
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
