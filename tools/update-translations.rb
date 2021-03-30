#!/usr/bin/env ruby

require 'fileutils'
require 'open-uri'
require 'uri'
require 'nokogiri'

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
en_file = File.join(RESDIR, 'values', 'strings.xml')
en_xml = File.open(en_file) { |f| Nokogiri::XML(f, nil, Encoding::UTF_8.to_s) }
exclude_attrs = ['name', 'content_override']
attr_map = en_xml.xpath('//string').map { |tag| [tag['name'], tag.attributes.reject { |k,_| exclude_attrs.include?(k) }] }.to_h

language_map.each do |gp_code, android_code, _|
  next if gp_code == 'en-us' # en-us is our base locale, no translations to download for it!

  puts "Downloading translations for '#{android_code}' from GlotPress (#{gp_code})..."
  lang_dir = File.join(RESDIR, "values-#{android_code}")
  lang_file = File.join(lang_dir, 'strings.xml')
  uri = URI.parse("#{GLOTPRESS_PROJECT_URL}/#{gp_code}/default/export-translations?filters[status]=current&format=android")
  begin
    xml = uri.open { |f| Nokogiri::XML(f.read.gsub("\t", '    '), nil, Encoding::UTF_8.to_s) }
    xml.xpath('//string').each do |string_tag|
      string_tag.content = string_tag.content.gsub('...', '…')
      # Copy the XML attributes from the original (en) strings.xml on the translated entry
      en_attrs = attr_map[string_tag['name']]
      en_attrs&.each { |k,v| string_tag[k] = v }
    end
    FileUtils.mkdir(lang_dir) unless Dir.exist?(lang_dir)
    File.open(lang_file, 'w:UTF-8') { |f| f.write(xml.to_xml(indent: 4)) }
  rescue StandardError => e
    puts "Error downloading #{gp_code} - #{e.message}"
    FileUtils.rm_rf(File.join(RESDIR, "values-#{android_code}"))
  end
end
