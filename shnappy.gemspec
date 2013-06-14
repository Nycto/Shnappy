Gem::Specification.new do |gem|

    # Generate a map of the keys defined in build.sbt
    build = File.open("build.sbt")
        .select { |line| line.include?(":=") }
        .inject({}) do |memo, line|
            parts = line.split(":=")
            memo[ parts[0].strip ] = parts[1].strip.gsub(/^"|"$/, "")
            memo
        end

    gem.name        = build["name"]
    gem.version     = build["version"]
    gem.platform    = Gem::Platform::RUBY
    gem.summary     = "A website built with Shnappy"
    gem.authors     = "Some People"

    gem.add_development_dependency('rake')
end

