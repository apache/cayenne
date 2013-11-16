TODO:

1. A reverse of @MixinRelationship - injecting mixin records into objects annotated with some mixin annotation.
2. Transactional auditable processing (with a mix of AuditableChild changes, multiple audit events are generated for the same object)

IMPLEMENTED:

5. @AuditableChild
4. Changeset tracking functionality
3. @MixinRelationship and MixinRelationshipFilter to batch-fault and inject related objects into mixin entity (e.g. Audit entity)
2. @Auditable mixin with abstract handler allowing to store audit records in an arbitrary format.
1. @Referenceable mixin and generic UUID processing classes
