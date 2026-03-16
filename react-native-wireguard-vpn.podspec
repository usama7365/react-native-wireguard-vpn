require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "react-native-wireguard-vpn"
  s.version      = package['version']
  s.summary      = package['description']
  s.description  = package['description']
  s.homepage     = package['homepage']
  s.license      = package['license']
  s.authors      = package['author']
  s.platforms    = { :ios => "12.0" }
  git_url        = package['repository']['url'].to_s.gsub(/^git\+/, '').gsub(/\.git$/, '')
  s.source       = { :git => git_url, :tag => "v#{s.version}" }
  s.source_files = "ios/**/*.{h,m,mm}"
  s.requires_arc = true
  s.dependency "React-Core"
  s.frameworks   = "NetworkExtension"
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES' }
end
