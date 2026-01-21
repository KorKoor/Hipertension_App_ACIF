package com.example.apphipertension
import java.time.LocalDate
object ConsejosDiarios {
    private val listaConsejos = listOf(
        "Reduce tu consumo de sal. La sal es el enemigo número uno de la presión arterial.",
        "Mide tu presión arterial a la misma hora todos los días. La consistencia es clave.",
        "Bebe suficiente agua. La hidratación adecuada ayuda a mantener el equilibrio en tu cuerpo.",
        "Realiza actividad física moderada, como caminar 30 minutos al día, la mayoría de los días de la semana.",
        "Evita el consumo excesivo de cafeína, ya que puede elevar temporalmente la presión.",
        "Asegúrate de dormir de 7 a 9 horas cada noche. Un buen descanso es vital para la salud cardiovascular.",
        "Practica la respiración profunda o la meditación para reducir el estrés diario.",
        "Consume alimentos ricos en potasio, como plátanos, espinacas o frijoles, para equilibrar el sodio.",
        "Limita el alcohol. El consumo moderado o nulo es lo mejor para tu presión arterial.",
        "Sigue tu plan de medicación al pie de la letra, incluso si te sientes bien. No la suspendas sin consultar.",
        "Si fumas, busca ayuda para dejarlo. Fumar daña seriamente tus vasos sanguíneos.",
        "Controla tu peso. Mantener un peso saludable reduce la tensión en tu corazón.",
        "Incrementa la fibra en tu dieta con cereales integrales y legumbres.",
        "Asegúrate de tener un espacio de tranquilidad cada día para desconectar y relajarte.",
        "Habla con tu médico sobre cualquier efecto secundario o cambio en tu estado de ánimo."
    )

    /**
     * Selecciona un consejo aleatorio pero fijo para el día actual.
     * Utiliza el día del año como semilla para garantizar que el consejo cambie solo una vez al día.
     */
    fun obtenerConsejoDelDia(): String {
        // Obtenemos el día del año (1 a 366)
        val dayOfYear = LocalDate.now().dayOfYear

        // Usamos el día del año módulo el tamaño de la lista para obtener un índice fijo
        val index = (dayOfYear - 1) % listaConsejos.size

        return listaConsejos[index]
    }
}