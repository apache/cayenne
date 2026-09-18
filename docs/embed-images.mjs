/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { readFileSync, existsSync, writeFileSync } from 'node:fs'
import { resolve, dirname, extname } from 'node:path'

const [htmlFile] = process.argv.slice(2)

if (!htmlFile) {
  throw new Error('Usage: node embed-images.mjs <html-file>')
}

const mimeTypes = {
  '.gif': 'image/gif',
  '.jpeg': 'image/jpeg',
  '.jpg': 'image/jpeg',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.webp': 'image/webp',
}

const htmlPath = resolve(htmlFile)
const htmlDirectory = dirname(htmlPath)
const html = readFileSync(htmlPath, 'utf8')
const embeddedHtml = html.replace(/src="([^\"]*\/_images\/[^\"]+)"/g, (match, source) => {
  if (/^[a-z][a-z\d+.-]*:/i.test(source)) {
    return match
  }

  const imagePath = resolve(htmlDirectory, source)
  const mimeType = mimeTypes[extname(imagePath).toLowerCase()]

  if (!mimeType) {
    throw new Error(`Unsupported image type: ${source}`)
  }

  if (!existsSync(imagePath)) {
    throw new Error(`Image not found: ${imagePath}`)
  }

  const image = readFileSync(imagePath).toString('base64')
  return `src="data:${mimeType};base64,${image}"`
})

writeFileSync(htmlPath, embeddedHtml)
