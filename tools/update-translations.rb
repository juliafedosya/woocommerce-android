#!/usr/bin/env ruby

require 'fileutils'
require 'open-uri'
require 'uri'

LANG_FILE=File.join(__dir__, 'exported-language-codes.csv')
RESDIR=File.join(__dir__, '..', 'WooCommerce', 'src', 'main', 'res')
LANGUAGE_DEF_FILE=File.join(RESDIR, 'values', 'available_languages.xml')
GLOTPRESS_PROJECT_URL='https://translate.wordpress.com/projects/woocommerce/woocommerce-android'

# Array<[GlotPress code, Android code, Locale name]>
language_map = File.readlines(LANG_FILE).reject { |l| l.strip.empty? }.map { |l| l.split(',') }


# Language definitions resource file
lang_list = language_map.map { |_, android_code, _| "<item>#{android_code.gsub('-r','_')}</item>" }
langs_list_xml = <<~XML
  <?xml version="1.0" encoding="UTF-8"?>
  <!--Warning: Auto-generated file, don't edit it.-->
  <resources>
  <string-array name="available_languages" translatable="false">
    #{lang_list.join("\n  ")}
  </string-array>
  </resources>
XML
File.write(LANGUAGE_DEF_FILE, langs_list_xml)


# Download translations from GlotPress
language_map.each do |gp_code, android_code, _|
  next if gp_code == 'en-us' # en-us is our base locale, no translations to download for it!

  puts "Downloading translations for '#{android_code}' from GlotPress (#{gp_code})..."
  lang_dir = File.join(RESDIR, "values-#{android_code}")
  uri = URI.parse("#{GLOTPRESS_PROJECT_URL}/#{gp_code}/default/export-translations?filters[status]=current&format=android")
  begin
    body = uri.read.force_encoding('UTF-8')
    strings_xml = body.gsub('...', '…').gsub("\t", '    ')
    FileUtils.mkdir(lang_dir) unless Dir.exist?(lang_dir)
    File.write(File.join(lang_dir, 'strings.xml'), strings_xml)
  rescue StandardError => e
    puts "Error downloading #{gp_code} - #{response.code}: #{response.message}"
    FileUtils.rm_rf(File.join(RESDIR, "values-#{android_code}"))
  end
end
