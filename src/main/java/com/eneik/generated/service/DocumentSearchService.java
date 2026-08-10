package com.eneik.generated.service;

import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
import com.eneik.generated.model.DocumentLmsMetadata;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.DocumentLmsMetadataRepository;
import com.eneik.generated.util.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentSearchService {

    private final DocumentRepository documentRepository;
    private final DocumentLmsMetadataRepository lmsMetadataRepository;
    private final TimeProvider timeProvider;

    private static final List<List<String>> SYNONYM_GROUPS = List.of(
        List.of("фгос", "федеральный государственный образовательный стандарт", "федерального государственного образовательного стандарта", "федеральному государственному образовательному стандарту", "федеральном государственном образовательном стандарте", "федеральные государственные образовательные стандарты"),
        List.of("гэк", "государственная экзаменационная комиссия", "государственной экзаменационной комиссии", "государственную экзаменационную комиссию", "государственные экзаменационные комиссии"),
        List.of("гиа", "государственная итоговая аттестация", "государственной итоговой аттестации", "государственную итоговую аттестацию", "государственные итоговые аттестации"),
        List.of("фбун", "федеральное бюджетное учреждение науки", "федерального бюджетного учреждения науки", "федеральному бюджетному учреждению науки", "федеральным бюджетным учреждением науки"),
        List.of("фвоко", "федеральная внутривузовская оценка качества образования", "федеральной внутривузовской оценки качества образования", "федеральная внешняя оценка качества образования", "федеральной внешней оценки качества образования", "внутренняя оценка качества образования", "внутренней оценки качества образования"),
        List.of("всоко", "внутренняя система оценки качества образования", "внутренней системы оценки качества образования", "внутреннюю систему оценки качества образования", "внутренней системой оценки качества образования"),
        List.of("фос", "фонд оценочных средств", "фонда оценочных средств", "фонду оценочных средств", "фондом оценочных средств", "фонды оценочных средств"),
        List.of("эиос", "электронная информационно-образовательная среда", "электронной информационно-образовательной среды", "электронную информационно-образовательную среду", "электронной информационно-образовательной средой"),
        List.of("сэд", "система электронного документооборота", "системы электронного документооборота", "систему электронного документооборота", "системой электронного документооборота"),
        List.of("лмс", "lms", "система управления обучением", "системы управления обучением", "систему управления обучением", "системой управления обучением"),
        List.of("цнии", "центральный научно-исследовательский институт", "центрального научно-исследовательного института", "центральному научно-исследовательскому институту", "центральным научно-исследовательским институтом")
    );

    public DocumentSearchService(DocumentRepository documentRepository, DocumentLmsMetadataRepository lmsMetadataRepository, TimeProvider timeProvider) {
        this.documentRepository = documentRepository;
        this.lmsMetadataRepository = lmsMetadataRepository;
        this.timeProvider = timeProvider;
    }

    public static class SuggestionsResult {
        private final List<String> suggestions;
        private final String typoCorrection;

        public SuggestionsResult(List<String> suggestions, String typoCorrection) {
            this.suggestions = suggestions;
            this.typoCorrection = typoCorrection;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public String getTypoCorrection() {
            return typoCorrection;
        }
    }

    @Transactional(readOnly = true)
    public SuggestionsResult getSuggestionsAndCorrections(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new SuggestionsResult(Collections.emptyList(), null);
        }

        String trimmedQuery = query.trim();
        String normalizedQuery = normalizeText(trimmedQuery);

        List<Document> allDocs = documentRepository.findAll();

        // 1. Suggestions List (up to 5 document titles containing the normalized query)
        List<String> suggestions = new ArrayList<>();
        if (trimmedQuery.length() >= 2) {
            for (Document doc : allDocs) {
                if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(normalizedQuery)) {
                    suggestions.add(doc.getTitle());
                }
                if (suggestions.size() >= 5) {
                    break;
                }
            }
        }

        // 2. Typo Correction
        String typoCorrection = null;
        if (trimmedQuery.length() >= 3) {
            // Check if there is an exact match (if so, we don't suggest correction)
            boolean hasExactMatch = false;
            for (Document doc : allDocs) {
                if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(normalizedQuery)) {
                    hasExactMatch = true;
                    break;
                }
                if (doc.getDocumentNumber() != null && doc.getDocumentNumber().toLowerCase().contains(normalizedQuery)) {
                    hasExactMatch = true;
                    break;
                }
            }

            if (!hasExactMatch) {
                String bestMatch = null;
                double bestSim = 0.0;

                for (Document doc : allDocs) {
                    if (doc.getTitle() == null) continue;
                    String titleLower = normalizeText(doc.getTitle());
                    String[] titleWords = titleLower.split("\\s+");
                    String[] queryWords = normalizedQuery.split("\\s+");

                    double docMaxSim = 0.0;
                    for (String qw : queryWords) {
                        if (qw.length() < 3) continue;
                        for (String tw : titleWords) {
                            if (tw.length() < 3) continue;
                            double sim = getFuzzySimilarity(qw, tw);
                            if (sim > docMaxSim) {
                                docMaxSim = sim;
                            }
                        }
                    }

                    if (docMaxSim > bestSim && docMaxSim > 0.5 && docMaxSim < 1.0) {
                        bestSim = docMaxSim;
                        bestMatch = doc.getTitle();
                    }
                }
                typoCorrection = bestMatch;
            }
        }

        return new SuggestionsResult(suggestions, typoCorrection);
    }

    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, String programFilter, String documentTypeFilter) {
        return search(query, programFilter, documentTypeFilter, null, null);
    }

    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, String programFilter, String documentTypeFilter, String educationLevelFilter, String updateDateFilter) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Parse program filter to List<String>
        List<String> programList = null;
        if (programFilter != null && !programFilter.trim().isEmpty()) {
            programList = new ArrayList<>();
            if ("postgraduate".equalsIgnoreCase(programFilter)) {
                programList.add("postgraduate");
                programList.add("both");
            } else if ("residency".equalsIgnoreCase(programFilter)) {
                programList.add("residency");
                programList.add("both");
            } else if ("both".equalsIgnoreCase(programFilter)) {
                programList.add("both");
            }
        }

        // Parse documentType to Enum
        com.eneik.generated.model.DocumentType docTypeEnum = null;
        if (documentTypeFilter != null && !documentTypeFilter.trim().isEmpty()) {
            for (com.eneik.generated.model.DocumentType t : com.eneik.generated.model.DocumentType.values()) {
                if (t.name().equalsIgnoreCase(documentTypeFilter)) {
                    docTypeEnum = t;
                    break;
                }
            }
        }

        // Normalize education level filter
        String eduLevel = (educationLevelFilter != null && !educationLevelFilter.trim().isEmpty()) ? educationLevelFilter : null;

        // Parse update date filter
        java.time.LocalDateTime updateDateStart = null;
        java.time.LocalDateTime updateDateEnd = null;
        if (updateDateFilter != null && !updateDateFilter.trim().isEmpty()) {
            java.time.LocalDateTime anchor = timeProvider.now();
            if ("7days".equalsIgnoreCase(updateDateFilter)) {
                updateDateStart = anchor.minusDays(7);
            } else if ("30days".equalsIgnoreCase(updateDateFilter)) {
                updateDateStart = anchor.minusDays(30);
            } else if ("year".equalsIgnoreCase(updateDateFilter)) {
                updateDateStart = java.time.LocalDateTime.of(anchor.getYear(), 1, 1, 0, 0);
                updateDateEnd = java.time.LocalDateTime.of(anchor.getYear(), 12, 31, 23, 59, 59);
            } else {
                try {
                    updateDateStart = java.time.LocalDateTime.parse(updateDateFilter);
                } catch (Exception e) {
                    try {
                        updateDateStart = java.time.LocalDate.parse(updateDateFilter).atStartOfDay();
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
        }

        // Query database directly with filters
        List<Document> filteredDocs = documentRepository.findWithFilters(
            programList,
            docTypeEnum,
            eduLevel,
            updateDateStart,
            updateDateEnd
        );

        // 2. Identify active synonym groups in the search query
        String normalizedQuery = normalizeText(query);
        List<List<String>> activeGroups = new ArrayList<>();
        List<String> queryWords = new ArrayList<>();

        // Parse query into words and detect matching synonym groups
        String[] tokens = normalizedQuery.split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            boolean inGroup = false;
            for (List<String> group : SYNONYM_GROUPS) {
                if (isTokenInGroup(token, group) || containsAnyPhrase(normalizedQuery, group)) {
                    if (!activeGroups.contains(group)) {
                        activeGroups.add(group);
                    }
                    inGroup = true;
                }
            }
            if (!inGroup) {
                queryWords.add(token);
            }
        }

        // 3. Compute relevance score for each document
        List<SearchResult> results = new ArrayList<>();
        for (Document doc : filteredDocs) {
            double score = calculateRelevance(doc, queryWords, activeGroups, normalizedQuery);
            if (score > 0.0) {
                results.add(new SearchResult(doc, score));
            }
        }

        // 4. Sort by relevance descending
        results.sort((r1, r2) -> Double.compare(r2.getRank(), r1.getRank()));
        return results;
    }

    private double calculateRelevance(Document doc, List<String> queryWords, List<List<String>> activeGroups, String fullQuery) {
        double score = 0.0;

        String title = normalizeText(doc.getTitle());
        String description = doc.getDescription() != null ? normalizeText(doc.getDescription()) : "";

        // Collect all version changes summaries
        List<String> summaries = new ArrayList<>();
        if (doc.getVersions() != null) {
            for (DocumentVersion ver : doc.getVersions()) {
                if (ver.getChangesSummary() != null) {
                    summaries.add(normalizeText(ver.getChangesSummary()));
                }
            }
        }

        // A. Match synonym groups
        for (List<String> group : activeGroups) {
            boolean titleMatched = false;
            boolean descMatched = false;
            boolean versionMatched = false;

            for (String synonym : group) {
                if (!titleMatched && title.contains(synonym)) {
                    score += 10.0;
                    titleMatched = true;
                }
                if (!descMatched && description.contains(synonym)) {
                    score += 3.0;
                    descMatched = true;
                }
                if (!versionMatched) {
                    for (String summary : summaries) {
                        if (summary.contains(synonym)) {
                            score += 1.0;
                            versionMatched = true;
                            break;
                        }
                    }
                }
            }
        }

        // B. Match individual query words (fuzzy matching)
        String[] titleWords = title.split("\\s+");
        String[] descWords = description.split("\\s+");

        for (String qw : queryWords) {
            double wordBestScore = 0.0;

            // Title fuzzy match
            for (String tw : titleWords) {
                if (tw.isEmpty()) continue;
                if (tw.equals(qw) || tw.contains(qw) || qw.contains(tw)) {
                    wordBestScore = Math.max(wordBestScore, 10.0);
                } else {
                    double sim = getFuzzySimilarity(qw, tw);
                    if (sim >= 0.75) {
                        wordBestScore = Math.max(wordBestScore, 10.0 * sim);
                    }
                }
            }

            // Description fuzzy match
            for (String dw : descWords) {
                if (dw.isEmpty()) continue;
                if (dw.equals(qw) || dw.contains(qw) || qw.contains(dw)) {
                    wordBestScore = Math.max(wordBestScore, 3.0);
                } else {
                    double sim = getFuzzySimilarity(qw, dw);
                    if (sim >= 0.75) {
                        wordBestScore = Math.max(wordBestScore, 3.0 * sim);
                    }
                }
            }

            // Versions fuzzy match
            for (String summary : summaries) {
                String[] verWords = summary.split("\\s+");
                for (String vw : verWords) {
                    if (vw.isEmpty()) continue;
                    if (vw.equals(qw) || vw.contains(qw) || qw.contains(vw)) {
                        wordBestScore = Math.max(wordBestScore, 1.0);
                    } else {
                        double sim = getFuzzySimilarity(qw, vw);
                        if (sim >= 0.75) {
                            wordBestScore = Math.max(wordBestScore, 1.0 * sim);
                        }
                    }
                }
            }

            score += wordBestScore;
        }

        // C. Additional boost for exact full query matching in title/description
        if (title.contains(fullQuery)) {
            score += 5.0;
        }
        if (description.contains(fullQuery)) {
            score += 2.0;
        }

        // D. Include matches on external LMS metadata (SDO/Teachbase)
        List<DocumentLmsMetadata> lmsList = lmsMetadataRepository.findByDocumentId(doc.getId());
        if (lmsList != null) {
            for (DocumentLmsMetadata lms : lmsList) {
                String prov = normalizeText(lms.getLmsProvider());
                String extId = normalizeText(lms.getExternalId());
                String metaJson = lms.getMetadataJson() != null ? normalizeText(lms.getMetadataJson()) : "";

                // Match synonym groups against LMS metadata
                for (List<String> group : activeGroups) {
                    for (String synonym : group) {
                        if (prov.contains(synonym) || extId.contains(synonym) || metaJson.contains(synonym)) {
                            score += 3.0;
                        }
                    }
                }

                // Match individual query words against LMS metadata
                for (String qw : queryWords) {
                    if (prov.contains(qw) || extId.contains(qw) || metaJson.contains(qw)) {
                        score += 2.0;
                    }
                }
            }
        }

        return score;
    }

    private boolean isTokenInGroup(String token, List<String> group) {
        for (String member : group) {
            if (member.contains(token) || token.contains(member)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyPhrase(String text, List<String> group) {
        for (String member : group) {
            if (text.contains(member)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
            .replaceAll("[^a-zа-я0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private double getFuzzySimilarity(String s1, String s2) {
        if (s1.length() < 3 || s2.length() < 3) return 0.0;
        int dist = getLevenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) dist / maxLen);
    }

    private int getLevenshteinDistance(String s1, String s2) {
        int[] dp = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) {
            dp[j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int temp = dp[j];
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[j] = prev;
                } else {
                    dp[j] = Math.min(Math.min(dp[j - 1], dp[j]), prev) + 1;
                }
                prev = temp;
            }
        }
        return dp[s2.length()];
    }

    public static class SearchResult {
        private final Document document;
        private final double rank;

        public SearchResult(Document document, double rank) {
            this.document = document;
            this.rank = rank;
        }

        public Document getDocument() {
            return document;
        }

        public double getRank() {
            return rank;
        }
    }
}
