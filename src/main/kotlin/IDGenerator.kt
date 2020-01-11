package dev.mee42
// checkDuplicate should return true if it's a duplicate
fun generateID(checkDuplicate: (ID) -> Boolean) :ID {
    var length = 5
    while(true){
        for(i in 0..100) {
            val random = (('a'..'z') + ('0'..'9')).shuffled().take(length).fold("") { a, b -> a + b }
            if(!checkDuplicate(random)) {
                return random
            }
        }
        length++
    }
}