package toro.sources.utils

fun String.getOptimizedUrl(width: Int? = null): String {
    if (!this.contains("res.cloudinary.com")) return this
    
    val parts = this.split("/upload/")
    if (parts.size != 2) return this
    
    val transform = mutableListOf("q_auto", "f_auto")
    width?.let { transform.add("w_$it,c_limit") }
    
    val transformString = transform.joinToString(",")
    return "${parts[0]}/upload/$transformString/${parts[1]}"
}