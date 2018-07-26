package com.lightningkite.kotlinx.reflection.plugin

class KxvSetupFunction(val qualifiedFunctionName: String, val files: List<KxvFile>) {
    fun write(): String {
        val fileImportsA = files.flatMap { it.classes }.joinToString("\n") { "import ${it.qualifiedName}" }
        val fileImportsB = files.flatMap { it.classes }.joinToString("\n") { "import ${it.qualifiedName}Reflection" }
        val inits = files.flatMap { it.classes }.joinToString("\n") { "    ${it.qualifiedName}::class.kxReflect = ${it.qualifiedName}Reflection" }
        return """
            package ${qualifiedFunctionName.substringBeforeLast('.')}

            import com.lightningkite.kotlinx.reflection.kxReflect

            $fileImportsA

            $fileImportsB

            fun ${qualifiedFunctionName.substringAfterLast('.')}(){
                $inits
            }
        """.trimIndent()
    }
}