#
# Build definition
#

require 'rubygems'
require 'set'
require 'sass'
require 'pp'
require 'securerandom'


# Asks for user input and returns the result
def getInput ( question )
    puts question
    response = STDIN.gets.strip
    puts
    response
end

# Asks the user a yes or no question
def getYesOrNo ( question )
    response = getInput("#{question} (y/n)")

    if response.downcase == "y"
        true
    elsif response.downcase == "n"
        false
    else
        puts "Invalid response\n"
        getYesOrNo( question )
    end
end


# Requires that the dotcloud command line interface is installed
task :dotcloudcli do
    unless system("which dotcloud > /dev/null")
        puts "DotCloud command line interface not found!"
        puts "Fixing this error might be as easy as running the following:"
        puts
        puts "    sudo apt-get install python-setuptools python-dev build-essential;"
        puts "    sudo easy_install pip && sudo pip install dotcloud;"
        puts "    dotcloud setup;"
        puts
        fail "Command not found: dotcloud"
    end
end


# Configures the dotcloud project
task :dotcloud => [ :dotcloudcli ] do
    # Fetch dotcloud account information
    name = getInput("Enter the dotCloud project name:")
    if getYesOrNo("Does this project already exist?")
        sh("cd build; dotcloud connect #{name}")
    else
        sh("cd build; dotcloud create #{name}")
    end
    puts
end


# Configures the cloudant configuration
task :cloudant => [ :dotcloudcli ] do
    username = getInput("Please enter your Cloudant user name:")
    apiKey = getInput("Please enter your Cloudant API key:")
    password = getInput("Please enter the password for that Cloudant API key:")
    database = getInput("Please enter your Cloudant database name:")
    sh("cd build; dotcloud env set " +
       "CLOUDANT_USER=#{username} " +
       "CLOUDANT_KEY=#{apiKey} " +
       "CLOUDANT_PASSWORD=#{password} " +
       "COUCHDB_DATABASE=#{database}")
    puts
end


# Sets up the "secrect" key for this instance
task :secret => [ :dotcloudcli ] do
    key = SecureRandom.uuid
    sh("cd build; dotcloud env set SECRET_KEY=#{key}")
    puts
end


# Initializes the dotcloud environment
task :setup => [ :dotcloud, :secret, :cloudant ] do

    # Reduce the WWW memory usage
    sh("cd build; dotcloud scale www:memory=256M")
end


# Compile the Sass
task :sass do
    puts "Compiling Sass..."
    sh("mkdir -p build/assets/css")

    includes = ["css"] + Gem::Specification.inject([]) do |memo, gem|
        dir = gem.gem_dir + "/app/assets/stylesheets"
        memo.push(dir) if File.exists?(dir)
        memo
    end

    puts "Sass include dirs:"
    pp(includes)

    Dir.glob('css/*')
        .select{ |file| [".sass", ".scss"].to_set.include?(File.extname(file)) }
        .reject{ |file| File.basename(file).start_with?("_") }
        .map do |file|
            withoutExt = file.chomp( File.extname(file) )
            compileTo = "build/assets/#{withoutExt}.css"

            puts "Compiling #{file} to #{compileTo}"

            engine = Sass::Engine.for_file(
                file,
                :syntax => :scss, :full_exception => true,
                :load_paths => includes
            )

            File.open(compileTo, 'w') { |file| file.write(engine.render) }
        end
end


# Copies all assets from the assets directory
task :assets do
    puts "Copying assets directory"
    FileUtils.cp_r( 'assets', 'build/' )
end


# Cleans out all build artifacts
task :clean do
    sh("sbt clean")
    FileUtils.rm_rf( 'build/templates' )
    FileUtils.rm_rf( 'build/assets' )
    FileUtils.rm( 'build/ROOT.war' )
end


# Builds the java WAR file
task :package do
    sh("sbt package-war")

    wars = Dir.glob('target/scala-*/*.war')
    if wars.length == 0
        throw "Could not locate packaged war file"
    elsif wars.length > 1
        throw "Found more than 1 war file. Consider doing a `rake clean`"
    end

    FileUtils.cp( wars[0], 'build/ROOT.war' )
    FileUtils.cp_r( 'templates', 'build/' )
end


# Default build behavior
task :default => [ :package, :assets ]


# Deploys this site out to dotcloud
task :deploy => [ :dotcloudcli, :default ] do
    sh("cd build; dotcloud push")
end

