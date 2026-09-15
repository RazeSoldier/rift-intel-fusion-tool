package cn.razesoldier.rift

import java.io.File

class SDEReader(val root: File) {
    fun readSolarSystems(): File {
        return root.resolve("mapSolarSystems.jsonl")
    }

    fun readConstellations(): File {
        return root.resolve("mapConstellations.jsonl")
    }

    fun readRegions(): File {
        return root.resolve("mapRegions.jsonl")
    }

    fun readPlanets(): File {
        return root.resolve("mapPlanets.jsonl")
    }

    fun readTypeGroups(): File {
        return root.resolve("groups.jsonl")
    }

    fun readMercenaryTacticalOperations(): File {
        return root.resolve("mercenaryTacticalOperations.jsonl")
    }

    fun readTypes(): File {
        return root.resolve("types.jsonl")
    }

    fun readStations(): File {
        return root.resolve("npcStations.jsonl")
    }

    fun readNpcCorporations(): File {
        return root.resolve("npcCorporations.jsonl")
    }

    fun readStationOperations(): File {
        return root.resolve("stationOperations.jsonl")
    }
}