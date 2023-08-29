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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import net.clementlevallois.umigon.model.Category;
import net.clementlevallois.umigon.model.PatternOfInterest;

/*
 *
 * @author C. Levallois
 */
public class PatternOfInterestChecker {

    private Set<PatternOfInterest> patternsOfInterest = ConcurrentHashMap.newKeySet();

    public  void loadPatternsOfInterest() throws IOException {

        try ( // we load the patterns of interest
                 InputStream inputStream = PatternOfInterestChecker.class.getResourceAsStream("patterns_of_interest.txt")) {
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
                toReturn = new PatternOfInterest();
                toReturn.setCategories(poiLoop.getCategories());
                toReturn.setTypeOfTextFragmentEnum(poiLoop.getTypeOfTextFragmentEnum());
                toReturn.setDescription(poiLoop.getDescription());
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
