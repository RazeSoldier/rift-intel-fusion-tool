package dev.nohus.rift.game

import dev.nohus.rift.corpprojects.Project
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository

object GameLink {

    fun forType(type: TypesRepository.Type): String {
        return "<url=showinfo:${type.id}>${type.name}</url>"
    }

    fun forSystem(system: MapSolarSystem): String {
        return "<url=showinfo:5//${system.id}>${system.name}</url>"
    }

    fun forCorporationProject(project: Project): String {
        return "<url=opportunity:corporation_goals:${project.id}>${project.name}</url>"
    }
}
