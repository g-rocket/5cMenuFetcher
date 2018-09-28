The project is build with `mvn`. To run the default build, cd into the base project directory and run mvn.
That should produce a jar file in `target/menufetcher-jar-with-dependencies.jar`.

To run that, run:

`java -jar target/menufetcher-jar-with-dependencies.jar --help`

---

The project is structured that, whenever that executable is run, it fetches and parses the menus from the dining halls
and outputs webpages and API pages. You can control exactly what it fetches and outputs with command-line arguments, as
follows (copied from the program help):

```
Option                                Description                              
------                                -----------                              
-?, -h, --help                        Get help                                 
-a, --api                             Generate the api                         
-d, --date, --dates <yyyy-MM-dd>      A list of dates to generate              
-e, --endDate, --to <yyyy-MM-dd>      The ending date to generate              
-f, --basedir <File>                  The base directory for the webpages and  
                                        `api` folder (default: .)              
-i, --index [yyyy-MM-dd]              Generate the index, optionally at a given
                                        date (default: 2018-08-28)             
-n, --numDays <Integer>               How many days to generate (default: 1)   
-r, --replace <none, blank, all>      Replace certain pages with menu-not-     
                                        available pages (default: none)        
-s, --from, --startDate <yyyy-MM-dd>  The starting date to generate (default:  
                                        2018-08-28)                            
-w, --web                             Generate the webpage                     
-x, --nomenu <yyyy-MM-dd:yyyy-MM-dd>  Generate menu-not-available pages        
-z, --interactive                     Interactively fetch only the requested   
                                        data and format or display it (for     
                                        debugging)                             
```