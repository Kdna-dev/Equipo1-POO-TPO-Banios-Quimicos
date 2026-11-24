package Modelo.Aplicacion;

import Modelo.Dominio.Banio;

/** Situación de una persona: en qué baño está esperando (o usando), su posición y el tiempo estimado. */
public record Seguimiento(Banio banio, boolean usandoBanio, int posicion, int personasDelante, long tiempoEstimadoMin) { }
