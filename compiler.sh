#!/bin/bash

# Script pour compiler le projet Jeu de Carré en Réseau

echo "Compilation des fichiers source Java..."

# Navigue dans le dossier source et compile tous les fichiers .java
javac src/jeux/*.java

# Vérifie si la compilation a réussi
if [ $? -eq 0 ]; then
    echo "Compilation terminée avec succès."
else
    echo "Erreur lors de la compilation."
fi