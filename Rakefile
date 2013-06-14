#
# Build definition
#


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


# Initializes the dotcloud environment
task :setup do

    # Make sure the dotcloud client is installed
    unless system("which dotcloud > /dev/null")
        puts "DotCloud command line interface not found!"
        puts "Fixing this error might be as easy as running the following:"
        puts
        puts "    sudo easy_install pip && sudo pip install dotcloud"
        puts
        fail "Command not found: dotcloud"
    end

    # Fetch dotcloud account information
    name = getInput("Enter the dotCloud project name:")
    if getYesOrNo("Does this project already exist?")
        sh("cd build; dotcloud connect #{name}")
    else
        sh("cd build; dotcloud create #{name}")
    end
    puts

    # Fetch cloudant database information
    username = getInput("Please enter your Cloudant user name:")
    apiKey = getInput("Please enter your Cloudant API key:")
    password = getInput("Please enter the password for that Cloudant API key:")
    database = getInput("Please enter your Cloudant database name:")
    sh("cd build; dotcloud env set " +
       "CLOUDANT_USER=#{username} " +
       "CLOUDANT_KEY=#{apiKey} " +
       "CLOUDANT_PASSWORD=#{password} " +
       "COUCHDB_DATABASE=#{database}")

end


# Cleans out all build artifacts
task :clean do
    sh("sbt clean")
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
end

# Deploys this site out to dotcloud
task :deploy => [ :package ] do
    sh("cd build; dotcloud push")
end

