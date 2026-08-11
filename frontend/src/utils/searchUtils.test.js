import { describe, it, expect } from 'vitest';
import { getTypoCorrection, getSuggestions, getFuzzySimilarity, getLevenshteinDistance } from './searchUtils.js';

describe('Search Auto-Suggestions and Typo Correction', () => {
  const mockDocuments = [
    { title: 'ФГОС ВО по специальности 32.08.12 Эпидемиология', documentNumber: 'ФГОС-32.08.12' },
    { title: 'Регламент проведения ГИА и кандидатских экзаменов', documentNumber: 'РЕГ-ГИА-2026' },
    { title: 'Вопросы к кандидатским экзаменам по профильным дисциплинам', documentNumber: 'ВОП-КАНД-2025' }
  ];

  it('calculates levenshtein distance correctly', () => {
    expect(getLevenshteinDistance('test', 'tost')).toBe(1);
    expect(getLevenshteinDistance('эпидемиология', 'эпидемеология')).toBe(1);
  });

  it('returns exact matching suggestions', () => {
    const suggestions = getSuggestions('эпидемиология', mockDocuments);
    expect(suggestions).toHaveLength(1);
    expect(suggestions[0]).toBe('ФГОС ВО по специальности 32.08.12 Эпидемиология');
  });

  it('suggests corrected queries for typos', () => {
    const correction = getTypoCorrection('эпидемеология', mockDocuments);
    expect(correction).toBe('ФГОС ВО по специальности 32.08.12 Эпидемиология');
  });

  it('does not suggest correction if exact match exists', () => {
    const correction = getTypoCorrection('эпидемиология', mockDocuments);
    expect(correction).toBeNull();
  });

  it('does not return suggestions for very short queries', () => {
    const suggestions = getSuggestions('э', mockDocuments);
    expect(suggestions).toHaveLength(0);
  });
});
