
def prettyPrint(level, folder) {
    print("\t"*level)
    println(folder.name)

    folder.folders.each { prettyPrint(level + 1, it)}
}

Gmail.folders.each{prettyPrint(0,it)}