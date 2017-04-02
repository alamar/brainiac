if [%1] == [] (
set /p model= "Enter model:"
)

java -Xmx1024M -classpath target/brainiac-1.0-SNAPSHOT.jar;target/dependency/* ru.lj.alamar.brainiac.Model %model% %*
