package org.apache.maven.archetype;

/** @author Jason van Zyl */
public class DefaultArchetypeFacade
    implements ArchetypeFacade
{
    public ArchetypeCreationResult createArchetypeFromProject( ArchetypeCreationRequest request )
    {
        ArchetypeCreationResult result = new ArchetypeCreationResult();

        return result;
    }

    public ArchetypeGenerationResult generateProjectFromArchetype( ArchetypeGenerationRequest request )
    {
        ArchetypeGenerationResult result = new ArchetypeGenerationResult();

        return result;
    }
}