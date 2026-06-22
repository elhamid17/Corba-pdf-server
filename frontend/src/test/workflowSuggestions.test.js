import { describe, it, expect } from 'vitest'
import {
  SUGGESTIONS_SOURCE_PATHS,
  SUGGESTIONS_TARGET_PATHS,
  WORKFLOW_SUGGESTIONS,
} from '../components/workflowSuggestionsMap'
import { SERVICES } from '../services'

const LEGACY_PATHS = new Set([
  '/watermark', '/stamp', '/extract-pages', '/delete-pages',
  '/qr', '/barcode', '/to-pdfa', '/resize', '/create',
])

const KNOWN_ROUTES = new Set([
  ...SERVICES.map(s => s.to),
  '/login', '/register', '/history', '/admin',
])

describe('workflow suggestions map', () => {
  it('source paths do not use legacy routes', () => {
    for (const path of SUGGESTIONS_SOURCE_PATHS) {
      expect(LEGACY_PATHS.has(path)).toBe(false)
    }
  })

  it('target paths do not use legacy routes', () => {
    for (const path of SUGGESTIONS_TARGET_PATHS) {
      expect(LEGACY_PATHS.has(path)).toBe(false)
    }
  })

  it('includes consolidated v2 source routes with suggestions', () => {
    expect(WORKFLOW_SUGGESTIONS['/marking'].length).toBeGreaterThan(0)
    expect(WORKFLOW_SUGGESTIONS['/select-pages'].length).toBeGreaterThan(0)
    expect(WORKFLOW_SUGGESTIONS['/code'].length).toBeGreaterThan(0)
    expect(WORKFLOW_SUGGESTIONS['/compress'].length).toBeGreaterThan(0)
  })

  it('target paths point to known service routes', () => {
    for (const path of SUGGESTIONS_TARGET_PATHS) {
      expect(KNOWN_ROUTES.has(path)).toBe(true)
    }
  })
})
