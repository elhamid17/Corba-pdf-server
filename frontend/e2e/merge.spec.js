import { test, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const fixturesDir = path.join(__dirname, 'fixtures')

/** PDF minimal valide pour simuler la reponse merge. */
const MERGED_PDF = Buffer.from(
  '%PDF-1.4\n1 0 obj<<>>endobj\nxref\n0 1\ntrailer<<>>\n%%EOF'
)

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('corba_pdf_onboarded', '1')
  })
})

test('merge two PDFs triggers download', async ({ page }) => {
  await page.route('**/api/pdf/merge', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/pdf',
      headers: { 'Content-Disposition': 'attachment; filename="merged.pdf"' },
      body: MERGED_PDF,
    })
  })

  const downloadPromise = page.waitForEvent('download')

  await page.goto('/merge')
  await expect(page.getByRole('heading', { name: 'Fusion de PDF' })).toBeVisible()

  await page.locator('input[type="file"]').setInputFiles([
    path.join(fixturesDir, 'a.pdf'),
    path.join(fixturesDir, 'b.pdf'),
  ])

  await page.getByRole('button', { name: /Fusionner/i }).click()

  const download = await downloadPromise
  expect(download.suggestedFilename()).toMatch(/merged\.pdf$/i)
  await expect(page.getByText(/Fusion réussie/i)).toBeVisible()
})

test('merge button disabled with fewer than two files', async ({ page }) => {
  await page.goto('/merge')

  await page.locator('input[type="file"]').setInputFiles([
    path.join(fixturesDir, 'a.pdf'),
  ])

  await expect(page.getByRole('button', { name: /Fusionner/i })).toBeDisabled()
})
