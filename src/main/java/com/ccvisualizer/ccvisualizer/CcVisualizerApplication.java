package com.ccvisualizer.ccvisualizer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CcVisualizerApplication {

	public static void main(String[] args) {
		try {
			String filePath = "src/main/java/com/ccvisualizer/ccvisualizer/index.html";
			String outputSvgFilePath = "src/main/java/com/ccvisualizer/ccvisualizer/output.svg";

			File input = new File(filePath);
			Document document = Jsoup.parse(input, "UTF-8");

			user_name userr= new user_name();

			details(userr.getUsername(), document);

			Element svgElement = convertToSvg(document);

			String svgContent = svgElement.outerHtml();

			File output = new File(outputSvgFilePath);
			writeToFile(svgContent, output);

			Files.write(Path.of(outputSvgFilePath), svgContent.getBytes(StandardCharsets.UTF_8));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void replaceInnerText(Document document, String elementId, String newValue) {
		Element element = document.getElementById(elementId);
		if (element != null) {
			element.text(newValue);
		} else {
			System.out.println("Element with ID '" + elementId + "' not found.");
		}
	}

	private static void replaceAttrValue(Document document, String elementId, String atr, String atrVal) {
		Element element = document.getElementById(elementId);
		if (element != null) {
			element.attr(atr, atrVal);
		} else {
			System.out.println("Element with ID '" + elementId + "' not found.");
		}
	}

	private static void writeToFile(String content, File file) throws IOException {
		org.apache.commons.io.FileUtils.writeStringToFile(file, content, "UTF-8");
	}

	private static Element convertToSvg(Document document) {
		Element svgElement = document.select("svg").first();
		return svgElement;
	}

	// CodeChef's rating-number / rating-header blocks bundle extra nested text
	// (a tooltip icon and "Provisional Rating, click to know more") inside the
	// same element as the number itself. These two helpers pull out just the
	// digits instead of trusting element.text() to be clean.

	private static String extractLeadingNumber(String raw) {
		if (raw == null) return "--";
		Matcher m = Pattern.compile("^(\\d+)").matcher(raw.trim());
		return m.find() ? m.group(1) : "--";
	}

	private static String extractDigits(String raw) {
		if (raw == null) return "--";
		Matcher m = Pattern.compile("(\\d+)").matcher(raw);
		return m.find() ? m.group(1) : "--";
	}

	public static void details(String username, Document document) throws IOException {
		Details user = new Details();

		String url = "https://www.codechef.com/users/" + username;
		Document doc = Jsoup.connect(url).get();

		user.setUsername(username);

		Element nameElement = doc.select("h1.h2-style").first();
		if (nameElement != null) {
			user.setName(nameElement.text());
		}

		Element currStarElement = doc.select("span.rating").first();
		if (currStarElement != null) {
			user.setCurrStar(currStarElement.text());
		}

		Elements instituteElements = doc.select("li:has(label:contains(Institution)) span");
		if (!instituteElements.isEmpty()) {
			user.setInstitute(instituteElements.first().text());
		}

		Element countryElement = doc.select("span.user-country-name").first();
		if (countryElement != null) {
			user.setCountry(countryElement.text());
		}

		// --- CodeChef Rating + DSA Rating ---
		// Two parallel rating blocks in document order:
		// index 0 = Contest ("CodeChef") rating, index 1 = DSA rating.
		Elements ratingNumberEls = doc.select("div.rating-number");
		Elements ratingHeaderSmallEls = doc.select(".rating-header.text-center small");
		List<Element> ranks = doc.select(".rating-ranks .inline-list strong");

		if (ratingNumberEls.size() > 0) {
			user.setCurrRating(extractLeadingNumber(ratingNumberEls.get(0).text()));
		}
		if (ratingNumberEls.size() > 1) {
			user.setDsaCurrRating(extractLeadingNumber(ratingNumberEls.get(1).text()));
		}

		if (ratingHeaderSmallEls.size() > 0) {
			user.setMaxRating(extractDigits(ratingHeaderSmallEls.get(0).text()));
		}
		if (ratingHeaderSmallEls.size() > 1) {
			user.setDsaMaxRating(extractDigits(ratingHeaderSmallEls.get(1).text()));
		}

		// [contestGlobal, contestCountry, dsaGlobal, dsaCountry]
		if (ranks.size() > 0) user.setGlobalRank(ranks.get(0).text());
		if (ranks.size() > 1) user.setCountryRank(ranks.get(1).text());
		if (ranks.size() > 2) user.setDsaGlobalRank(ranks.get(2).text());
		if (ranks.size() > 3) user.setDsaCountryRank(ranks.get(3).text());

		Element contestParticipatedElement = doc.select("div.contest-participated-count b").first();
		if (contestParticipatedElement != null) {
			user.setContestParticipated(contestParticipatedElement.text());
		}

		Element styleElement = doc.select("span.rating").first();
		if (styleElement != null) {
			String style = styleElement.attr("style");
			if (style != null) {
				String[] styleParts = style.split(";");
				if (styleParts.length > 2) {
					user.setCurr_col(styleParts[2].split(": ")[1]);
				}
			}
		}

		String star_col = user.colorFind(user.getMaxRating());
		if (star_col != null) {
			String[] star_colParts = star_col.split(";");
			if (star_colParts.length > 1) {
				user.setMxStar(star_colParts[0]);
				user.setMx_col(star_colParts[1]);
			}
		}

		String dsaCurrStarCol = user.colorFind(user.getDsaCurrRating());
		if (dsaCurrStarCol != null) {
			String[] parts = dsaCurrStarCol.split(";");
			if (parts.length > 1) {
				user.setDsaCurrStar(parts[0]);
				user.setDsaCurr_col(parts[1]);
			}
		}
		String dsaMaxStarCol = user.colorFind(user.getDsaMaxRating());
		if (dsaMaxStarCol != null) {
			String[] parts = dsaMaxStarCol.split(";");
			if (parts.length > 1) {
				user.setDsaMxStar(parts[0]);
				user.setDsaMx_col(parts[1]);
			}
		}

		Element totalSolvedElement = doc.select("section.problems-solved h3:contains(Total Problems Solved)").first();
		if (totalSolvedElement != null) {
			user.setAccepted(String.valueOf(extractNumericValue(totalSolvedElement.text())));
		}

		replaceInnerText(document, "name", username);
		replaceInnerText(document, "user-curr", user.getName() + " ");
		replaceInnerText(document, "curr", user.getCurrStar() + " Rated ");
		replaceInnerText(document, "curr-r", user.getCurrRating());
		replaceInnerText(document, "t-contest", user.getContestParticipated());
		replaceInnerText(document, "max-r", user.getMaxRating());
		replaceInnerText(document, "max", user.getMxStar() + " Rated");
		replaceAttrValue(document, "max", "style", "color: " + user.getMx_col() + " ");
		replaceAttrValue(document, "curr", "style", "color: " + user.getCurr_col() + " ");
		replaceAttrValue(document, "curr-r", "style", "color: " + user.getCurr_col() + " ");
		replaceAttrValue(document, "max-r", "style", "color: " + user.getMx_col());
		replaceInnerText(document, "clg", user.getInstitute() + " | " + user.getCountry());
		replaceInnerText(document, "c-rank", user.getCountryRank());
		replaceInnerText(document, "g-rank", user.getGlobalRank());
		replaceInnerText(document, "accepted", user.getAccepted());
		replaceAttrValue(document, "max-r", "style", "color: " + user.getMx_col());
		replaceAttrValue(document, "curr-r", "style", "color: " + user.getCurr_col());
		replaceAttrValue(document, "name", "style", "color: " + user.getCurr_col() + " ");
		replaceAttrValue(document, "curr-r-name", "style", "color: " + user.getCurr_col() + " ");
		replaceAttrValue(document, "max-r-name", "style", "color: " + user.getMx_col() + " ");

		replaceInnerText(document, "dsa-curr-r", user.getDsaCurrRating());
		replaceInnerText(document, "dsa-max-r", user.getDsaMaxRating());
		replaceAttrValue(document, "dsa-curr-r", "style", "color: " + user.getDsaCurr_col() + " ");
		replaceAttrValue(document, "dsa-max-r", "style", "color: " + user.getDsaMx_col());
		replaceAttrValue(document, "dsa-curr-r-name", "style", "color: " + user.getDsaCurr_col() + " ");
		replaceAttrValue(document, "dsa-max-r-name", "style", "color: " + user.getDsaMx_col() + " ");

		System.out.println(user.toString());
	}

	public static int extractNumericValue(String input) {
		if (input == null) {
			return 0;
		}
		String numericString = input.replaceAll("[^0-9]", "");

		if (!numericString.isEmpty()) {
			return Integer.parseInt(numericString);
		} else {
			return 0;
		}
	}

}
