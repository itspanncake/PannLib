package fr.panncake.pannlib.orm.orm.entity;

public record ManagedEntity(Object entity, EntityState state, Object[] snapshot) {
}
