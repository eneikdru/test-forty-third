package com.eneik.generated.service;

import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentVersion;
import com.eneik.generated.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentSearchService {

    private final DocumentRepository documentRepository;

    private static final List<List<String>> SYNONYM_GROUPS = List.of(
        List.of("фгос", "федеральный государственный образовательный стандарт", "федерального государственного образовательного стандарта", "федеральному государственному образовательному стандарту", "федеральном государственном образовательном стандарте", "федеральные государственные образовательные стандарты"),
        List.of("гэк", "государственная экзаменационная комиссия", "государственной экзаменационной комиссии", "государственную экзаменационную комиссию", "государственные экзаменационные комиссии"),
        List.of("гиа", "государственная итоговая аттестация", "государственной итоговой аттестации", "государственную итоговую аттестацию", "государственные итоговые аттестации"),
        List.of("фбун", "федеральное бюджетное учреждение науки", "федерального бюджетного учреждения науки", "федеральному бюджетному учреждению науки", "федеральным бюджетным учреждением науки")
    );

    public DocumentSearchService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, String programFilter, String documentTypeFilter) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> allDocs = documentRepository.findAll();

        // 1. Filter by program and documentType
        List<Document> filteredDocs = allDocs.stream()
            .filter(doc -> {
                if (programFilter != null && !programFilter.trim().isEmpty()) {
                    String p = doc.getProgram();
                    if ("postgraduate".equalsIgnoreCase(programFilter)) {
                        return "postgraduate".equalsIgnoreCase(p) || "both".equalsIgnoreCase(p);
                    } else if ("residency".equalsIgnoreCase(programFilter)) {
                        return "residency".equalsIgnoreCase(p) || "both".equalsIgnoreCase(p);
                    } else if ("both".equalsIgnoreCase(programFilter)) {
                        return "both".equalsIgnoreCase(p);
                    }
                }
                return true;
            })
            .filter(doc -> {
                if (documentTypeFilter != null && !documentTypeFilter.trim().isEmpty()) {
                    return documentTypeFilter.equalsIgnoreCase(doc.getDocumentType());
                }
                return true;
            })
            .collect(Collectors.toList());

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
