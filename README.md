spookystuff
===========

(OR: how to turn 21st century into an open spreadsheet) is a scalable query engine for web scrapping/data mashup/acceptance QA powered on Apache Spark. The goal is to allow the Web being queried and ETL'ed as if it is a database.

Dependencies
-----------
- Apache Spark
- Selenium
    - GhostDriver/PhantomJS (for Google Chrome client simulation)
- JSoup
- Apache Tika (only for non-html parsing)
- (build tool) Apache Maven
    - Scala/ScalaTest plugins
- (deployment tool) Ansible
- Current implementation is influenced by Spark SQL and Mahout Sparkbinding.

Examples
-----------
#### 1. Search on LinkedIn
- Goal: Find high-ranking professionals in you area on [http://www.linkedin.com/], whose first name is either 'Sanjay', 'Arun' or 'Hardik', and last name is either 'Gupta' or 'Krishnamurthy', print out their full names, titles and lists of skills
- Query:
```
    (sc.parallelize(Seq("Sanjay", "Arun", "Hardik")) +>
      Visit("https://www.linkedin.com/") +>
      TextInput("input#first", "#{_}") +*>
      Seq( TextInput("input#last", "Gupta"), TextInput("input#last", "Krishnamurthy")) +>
      Submit("input[name=\"search\"]") !)
      .wgetJoin("ol#result-set h2 a") //faster
      .map{ page => (
      page.text1("span.full-name"),
      page.text1("p.title"),
      page.text("div#profile-skills li")
      )
    }.collect().foreach(println(_))
```
- Result (truncated, finished in 1 minutes on a laptop with ~400k/s wifi):
```
(Abhishek Arun Gupta,President & Senior IT Expert / Joint Chairman - IT Cell at Agra User Group / National Chamber of Industries & Commerce,ArrayBuffer(Requirements Analysis, SQL, Business Intelligence, Unix, Testing, President & Senior IT Expert, Joint Chairman - IT Cell, Quality Assurance (QA) & Automation Systems, Senior Automation Testing Expert, Senior Executive, Industry Interface))
(hardik gupta,--,ArrayBuffer())
(Arun Gupta,Sales at adjust by adeven,ArrayBuffer(Mobile, Business Strategy, Digital Media, Advertising Sales, Direct Sales, New Business Development, Mobile Marketing, Mobile Advertising, Publishing, Mobile Devices, Strategic Partnerships, Start-ups, Online Marketing, Mobile Applications, SEO, SEM, Business Development, Social Networking, Digital Marketing, Management, Digital Strategy))
(Dr. Sanjay Gupta,Co-Founder & Director at IMPACT PSD Private Limited,ArrayBuffer(Computer proficiency, Secondary Research, Operations Management, Qualitative Research, Research and M&E, Data Management, Data Interpretation, M&E, Research, Report Writing, Data Analysis, Proposal Writing, Program Management, Capacity Building, NGOs, Leadership, Market Research, Policy, Civil Society, International Development, Nonprofits, Public Policy, Corporate Social Responsibility, Training, Program Evaluation, Analysis, Business Development, Sustainable Development, Data Collection, Technical Assistance, Organizational Development, Fundraising, Community Development, Quantitative Research, Government, Program Development, Policy Analysis, Reproductive Health))
(Dr. Arun Kumar Gupta,Chief Executive Officer,ArrayBuffer())
... (75 lines)
```

#### 2. Query Machine Parts Database
- Goal: Given a washing machine model 'A210S', search on [http://www.appliancepartspros.com/] for the model's full name,  a list of schematic descriptions (with each one describing a subsystem), for each schematic, search for data of all enumerated machine parts: their description/manufacturer/OEM number, and a list of each one's substitutes. Join them all together and print them out.
- Query:
```
    (sc.parallelize(Seq("A210S")) +>
      Visit("http://www.appliancepartspros.com/") +>
      TextInput("input.ac-input","#{_}") +>
      Click("input[value=\"Search\"]") +> //TODO: can't use Submit, why?
      Delay(10) ! //TODO: change to DelayFor to save time
      ).selectInto(
        "model" -> { _.text1("div.dgrm-lst div.header h2") },
        "time1" -> { _.backtrace.last.timeline.asInstanceOf[Serializable] } //ugly tail
      ).wgetJoin("div.inner li a:has(img)")
      .selectInto("schematic" -> {_.text1("div#ctl00_cphMain_up1 h1 span")})
      .wgetJoin("tbody.m-bsc td.pdct-descr h2 a")
      .map(
        page => (
          page.context.get("_"),
          page.context.get("time1"),
          page.context.get("model"),
          page.context.get("schematic"),
          page.text1("div.m-pdct h1"),
          page.text1("div.m-pdct td[itemprop=\"brand\"] span"),
          page.text1("div.m-bsc div.mod ul li:contains(Manufacturer) strong"),
          page.text1("div.m-pdct div.m-chm p")
          )
      ).collect().foreach(println(_))
```
- Result (truncated, process finished in 2 minutes on one r3.large instance):
```
(A210S,A210S Washer-Top Loading ,07-Transmissions Parts for Maytag A210S,Collar-Dri,Whirlpool,Y014839,Part Number Y014839 (AP4277202) replaces 014839, 14839.)
(A210S,A210S Washer-Top Loading ,08-Transmissions Parts for Maytag A210S,Collar-Dri,Whirlpool,Y014839,Part Number Y014839 (AP4277202) replaces 014839, 14839.)
(A210S,A210S Washer-Top Loading ,05-Suds Saver Parts for Maytag A210S,Screw, Strainer to Pump,Maytag,911266,null)
... (311 lines)
```

#### 3. Download University Logos
- Goal: Search for Logos of all US Universities on Google Image (a list of US Universities can be found at [http://www.utexas.edu/world/univ/alpha/]), download them to one of your s3 bucket.
    - You need to set up your S3 credential by environment variables
    - The following query will crawl 4000+ page and web resources so its better to test it on a cluster
- Query:
```
    val names = ((sc.parallelize(Seq("dummy")) +>
      Visit("http://www.utexas.edu/world/univ/alpha/") !)
      .flatMap(_.text("div.box2 a", limit = Int.MaxValue, distinct = true))
      .repartition(400) +> //importantissimo! otherwise will only have 2 partitions
      Visit("http://images.google.com/") +>
      DelayFor("form[action=\"/search\"]",50) +>
      TextInput("input[name=\"q\"]","#{_} Logo") +>
      Submit("input[name=\"btnG\"]") +>
      DelayFor("div#search",50) !)
      .wgetJoin("div#search img",1,"src")
      .save("#{_}", "s3n://$[insert your bucket here],default to 'college-logo'$")
      .foreach(println(_))
```
- Result (process finished in 13 mintues on 4 r3.large instance, image files can be downloaded from S3 with a file transfer client supporting S3 (e.g. S3 web UI, crossFTP): 
```
    
```

Deployment
-----------------------------------------
### ... to Local Computer/Single Node
1. Install Apache Spark 1.0.0 from [http://spark.apache.org/downloads.html](http://spark.apache.org/downloads.html)
2. (Optional, highly recommended otherwise you have to set it everytime before running the shell or application) Edit your startup script to point the environment variable of Spark to your Spark installation directory:
    - export SPARK_HOME=*your Spark installation directory*
3. Install PhantomJS 1.9.7 from [http://phantomjs.org/download.html]
    - recommended to install to '/usr/lib/phantomjs', otherwise please change *phantomJSRootPath* in *org.tribbloid.spookystuff.Conf.scala* to point to your PhantomJS directory and recompile.
    - also provided by Ubuntu official repository (so you can apt-get it) but current binary is severely obsolete (1.9.0), use of this binary is NOT recommended and may cause unpredictable error.
4. git clone this repository.
5. MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512M -XX:ReservedCodeCacheSize=512m" mvn package -DskipTest=true
    - increasing jvm heapspace size for Apache Maven is mandatory as 2 modules (example and shell) will generate uber jars.
6. That's it! Now you have 3 options to use it:
    - (easiest) launch spooky-shell and improvise your query: bin/spooky-shell.sh
    - give any example a test run: bin/submit-example.sh *name of the example*
    - write your own application by importing spooky-core into your dependencies.

### ... to Cluster and Amazon EC2
1. Setup a cluster and assure mutual connectivity
2. Install Ubuntu 12+ on all nodes.
    - scripts to autodeploy on other Spark-compatible OS is currently NOT under active development. Please vote on the issue tracker if you demand it.
    - the easiest way to set it up is on Amazon EC2, AMI with pre-installed environment and autoscaling ability will be made public shortly
3. Install Ansible on your client and make sure you can ssh into all your nodes with a valid private key (id_rsa).
4. Edit files in ops/ansible/inventories.template to include ip/dns of your master node and all worker nodes. Change the directory name to /ops/ansible/inventories
5. cd into ops/ansible and:
    - deploy master: ./ansible-playbook deploy-master.yml -i ./inventories --private-key=*yor private key (id_rsa)*
    - deploy workers: .ansible-ploybook deploy-worker.yml -i ./inventories --private-key=*yor private key (id_rsa)*
    - this will install oracle-java7 and do step 1,2,3, automatically on all nodes. You can do it manually but that's a lot of work!
6. Do step 4,5 on master node and run any of the 3 options
    - you can download and run it on any node in the same subnet of the cluster, but expect heavy traffic between your client node and master node.

### alternatively ...
you can use scripts in $SPARK_HOME/ec2 to setup a Spark cluster with transient HDFS support. But this has 2 problems:
    - Autoscaling is currently not supported.
    - Spark installation directory is hardcoded to '/root/spark', if your client has a different directory it may cause some compatibility issue.

Query/Programming Guide
-----------
[This is a stub]

So far spookystuff only supports LINQ style query language, APIs are not finalized (in fact, still far from that) and may change in the future.

I'm trying to make the query language similar to the language-integrated query of Spark SQL. However, as organizations of websites are fundamentally different from relational databases, it may gradually evolve to attain maximum succinctness.

If you want to write extension for this project, MAKE SURE you don't get *NotSerializableException* in a local run (it happens when Spark cannot serialize data when sending to another node), and keep all RDD entity's serialization footprint small to avoid slow partitioning over the network.

Maintainer
-----------
(if you see a bug these are the guys/girls to blame for)

- @tribbloid (pc175@uow.edu.au)