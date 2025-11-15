package fr.panncake.pannlib.orm.entity;

public record ManagedEntity(Object entity, EntityState state, Object[] snapshot) {
}
