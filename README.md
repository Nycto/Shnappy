Shnappy
=======

A simple website renderer built on dotCloud and Cloudant.

What you need
-------------

1. A dotCloud account: https://www.dotcloud.com
2. A Cloudant account: https://cloudant.com

How to Build
------------

1. Make sure SBT is installed. Setup instructions are available here:

    http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html

2. Make sure Ruby is installed:

    ```
    sudo apt-get install ruby-full build-essential;
    ```

3. Install Bundler:

    ```
    gem install bundler;
    bundle install;
    ```

4. Then configure your site:

    ```
    bundle exec rake setup;
    ```

5. Deploy your code:

    ```
    bundle exec rake deploy;
    ```
    
