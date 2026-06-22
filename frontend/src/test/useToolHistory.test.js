import { describe, it, expect, beforeEach } from 'vitest'
import { recordVisit, toggleFavorite, useRecent, useFavorites } from '../hooks/useToolHistory'
import { renderHook, act } from '@testing-library/react'

describe('useToolHistory', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('recordVisit stores most recent path first', () => {
    recordVisit('/merge')
    recordVisit('/split')

    const { result } = renderHook(() => useRecent(4))
    expect(result.current[0].path).toBe('/split')
    expect(result.current[1].path).toBe('/merge')
  })

  it('recordVisit ignores home path', () => {
    recordVisit('/')
    recordVisit('/merge')

    const { result } = renderHook(() => useRecent(4))
    expect(result.current).toHaveLength(1)
    expect(result.current[0].path).toBe('/merge')
  })

  it('toggleFavorite adds and removes paths', () => {
    toggleFavorite('/ocr')
    let { result } = renderHook(() => useFavorites())
    expect(result.current.some(f => f.path === '/ocr')).toBe(true)

    act(() => toggleFavorite('/ocr'))
    ;({ result } = renderHook(() => useFavorites()))
    expect(result.current.some(f => f.path === '/ocr')).toBe(false)
  })
})
