import { describe, it, expect } from 'vitest'
import { SERVICES, CATEGORIES, getServiceByPath } from '../services'

describe('services catalog', () => {
  it('exposes one entry per tool path', () => {
    const paths = SERVICES.map(s => s.to)
    expect(new Set(paths).size).toBe(paths.length)
  })

  it('getServiceByPath resolves known routes', () => {
    const merge = getServiceByPath('/merge')
    expect(merge).not.toBeNull()
    expect(merge.title).toBe('Fusion')
    expect(merge.category).toBe('Organisation')
  })

  it('getServiceByPath returns null for unknown routes', () => {
    expect(getServiceByPath('/unknown-tool')).toBeNull()
  })

  it('every service belongs to a declared category or Tous filter', () => {
    const allowed = new Set(CATEGORIES.filter(c => c !== 'Tous'))
    for (const s of SERVICES) {
      expect(allowed.has(s.category)).toBe(true)
    }
  })

  it('includes consolidated v2 routes', () => {
    expect(getServiceByPath('/marking')).not.toBeNull()
    expect(getServiceByPath('/select-pages')).not.toBeNull()
    expect(getServiceByPath('/code')).not.toBeNull()
    expect(getServiceByPath('/scan')).not.toBeNull()
  })
})
