package com.streamvault.data.remote.f1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JolpicaRaceTableResponse(
    @SerialName("MRData") val mrData: JolpicaMRData
)

@Serializable
data class JolpicaMRData(
    @SerialName("RaceTable") val raceTable: JolpicaRaceTable
)

@Serializable
data class JolpicaRaceTable(
    @SerialName("Races") val races: List<JolpicaRace> = emptyList()
)

@Serializable
data class JolpicaRace(
    val round: String,
    val raceName: String,
    @SerialName("Circuit") val circuit: JolpicaCircuit,
    val date: String,
    val time: String? = null,
    @SerialName("FirstPractice") val firstPractice: JolpicaSessionTime? = null,
    @SerialName("SecondPractice") val secondPractice: JolpicaSessionTime? = null,
    @SerialName("ThirdPractice") val thirdPractice: JolpicaSessionTime? = null,
    @SerialName("Qualifying") val qualifying: JolpicaSessionTime? = null,
    @SerialName("Sprint") val sprint: JolpicaSessionTime? = null,
    @SerialName("SprintQualifying") val sprintQualifying: JolpicaSessionTime? = null
)

@Serializable
data class JolpicaSessionTime(
    val date: String,
    val time: String
)

@Serializable
data class JolpicaCircuit(
    val circuitName: String,
    @SerialName("Location") val location: JolpicaLocation
)

@Serializable
data class JolpicaLocation(
    val country: String,
    val locality: String? = null
)
