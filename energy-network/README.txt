Compiler le projet :
mvn clean install

Run le projet :
java -jar target/energy-network-1.0-SNAPSHOT.jar

Générer les graphes :
dot -Tpng src/main/results/result-arbre[NBR].dot > src/main/results/dot[NBR].png
Où [NBR] est le nombre de prosumers, le fichier xml doit exister

[NBR] doit être indiqué dans le main du programme pour générer les dots.