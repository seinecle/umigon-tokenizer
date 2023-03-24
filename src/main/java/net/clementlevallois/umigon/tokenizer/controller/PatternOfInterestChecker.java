/*
 * To change this template, choose Toolc | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.umigon.tokenizer.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import net.clementlevallois.umigon.model.Category;
import net.clementlevallois.umigon.model.PatternOfInterest;

/*
 *
 * @author C. Levallois
 */
public class PatternOfInterestChecker {

    private List<PatternOfInterest> patternsOfInterest = new ArrayList();

    public  void loadPatternsOfInterest() throws IOException {

        try ( // we load the patterns of interest
                 InputStream inputStream = PlaceHolderMULTI.class.getResourceAsStream("patterns_of_interest.txt")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            List<String> patternsOfInterestAsTSV = br.lines().collect(Collectors.toList());
            PatternOfInterest poi;
            int i = 0;
            for (String patternOfInterestAsTSV : patternsOfInterestAsTSV) {
                if (i++ == 0) {
                    continue;
                }
                String[] elements = patternOfInterestAsTSV.split("\t");
                poi = new PatternOfInterest();
                poi.setDescription(elements[0]);
                poi.setRegex(elements[1]);
                poi.setShouldApplyToLowerCaseText(Boolean.parseBoolean(elements[2]));
                List<String> categoriesIds = Arrays.asList(elements[3].split(","));
                for (String categoryId : categoriesIds) {
                    Category category = new Category(categoryId);
                    poi.getCategories().add(category);
                }
                if (elements[4] == null) {
                    System.out.println("stop");
                }
                poi.setTypeOfTextFragment(elements[4]);
                patternsOfInterest.add(poi);
            }
        }
    }

    public String containsPercentage(String text) {
        //do we find a percentage?
        boolean res = text.matches(".*\\d%.*");
        if (res) {
            //if so, is it followed by "off"?
            res = (text.toLowerCase().matches(".*\\d% off.*") | text.toLowerCase().matches(".*\\d% cash back.*"));
            if (res) {
                return "611";
            } else {
                return "621";
            }
        }
        return null;
    }

    public PatternOfInterest returnsMatchOrNot(String text) {
        Matcher matcher;
        PatternOfInterest toReturn = null;
        for (PatternOfInterest poiLoop : patternsOfInterest) {
            matcher = poiLoop.getPattern().matcher(text);
            if (matcher.matches()) {
                toReturn = poiLoop;
                toReturn.setMatched(Boolean.TRUE);
            }
        }
        if (toReturn == null) {
            PatternOfInterest poi = new PatternOfInterest();
            poi.setMatched(Boolean.FALSE);
            return poi;
        } else {
            return toReturn;
        }
    }
}
