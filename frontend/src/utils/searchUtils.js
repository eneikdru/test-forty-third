export function getLevenshteinDistance(s1, s2) {
  const dp = Array(s2.length + 1).fill(0).map((_, i) => i);
  for (let i = 1; i <= s1.length; i++) {
    let prev = dp[0];
    dp[0] = i;
    for (let j = 1; j <= s2.length; j++) {
      const temp = dp[j];
      if (s1[i - 1] === s2[j - 1]) {
        dp[j] = prev;
      } else {
        dp[j] = Math.min(dp[j - 1], dp[j], prev) + 1;
      }
      prev = temp;
    }
  }
  return dp[s2.length];
}

export function getFuzzySimilarity(s1, s2) {
  if (s1.length < 3 || s2.length < 3) return 0.0;
  const words1 = s1.split(/\s+/);
  const words2 = s2.split(/\s+/);

  let maxWordSim = 0.0;
  for (const w1 of words1) {
    if (w1.length < 3) continue;
    for (const w2 of words2) {
      if (w2.length < 3) continue;
      const dist = getLevenshteinDistance(w1, w2);
      const maxLen = Math.max(w1.length, w2.length);
      const sim = 1.0 - dist / maxLen;
      if (sim > maxWordSim) {
        maxWordSim = sim;
      }
    }
  }
  return maxWordSim;
}

export function getTypoCorrection(query, documents) {
  const q = query.trim();
  if (!q || q.length < 3) return null;
  const normalizedQ = q.toLowerCase();

  const hasExactMatch = documents.some(doc =>
    doc.title.toLowerCase().includes(normalizedQ) ||
    (doc.documentNumber && doc.documentNumber.toLowerCase().includes(normalizedQ))
  );
  if (hasExactMatch) return null;

  let bestMatch = null;
  let bestSim = 0.0;

  for (const doc of documents) {
    const title = doc.title;
    const sim = getFuzzySimilarity(normalizedQ, title.toLowerCase());
    if (sim > bestSim && sim > 0.5 && sim < 1.0) {
      bestSim = sim;
      bestMatch = title;
    }
  }
  return bestMatch;
}

export function getSuggestions(query, documents, limit = 5) {
  const q = query.trim();
  if (!q || q.length < 2) return [];
  const normalizedQ = q.toLowerCase();

  return documents
    .filter(doc => doc.title.toLowerCase().includes(normalizedQ))
    .slice(0, limit)
    .map(doc => doc.title);
}
