package org.hisp.dhis.web.webapi.v1.utils;

/*
 * Copyright (c) 2004-2012, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the HISP project nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.web.webapi.v1.controller.FacilityController;
import org.hisp.dhis.web.webapi.v1.domain.Facility;
import org.hisp.dhis.web.webapi.v1.domain.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class OrganisationUnitToFacilityConverter implements Converter<OrganisationUnit, Facility>
{
    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Override
    public Facility convert( OrganisationUnit organisationUnit )
    {
        Facility facility = new Facility();
        facility.setId( organisationUnit.getUuid() );
        facility.setName( organisationUnit.getDisplayName() );
        facility.setActive( organisationUnit.isActive() );
        facility.setCreatedAt( organisationUnit.getCreated() );
        facility.setUpdatedAt( organisationUnit.getLastUpdated() );
        facility.setUrl( linkTo( FacilityController.class ).slash( organisationUnit.getUid() ).toString() );

        if ( organisationUnit.getFeatureType() != null && organisationUnit.getFeatureType().equalsIgnoreCase( "POINT" )
            && organisationUnit.getCoordinates() != null )
        {
            try
            {
                GeoUtils.Coordinates coordinates = GeoUtils.parseCoordinates( organisationUnit.getCoordinates() );

                facility.getCoordinates().add( coordinates.lng );
                facility.getCoordinates().add( coordinates.lat );
            }
            catch ( NumberFormatException ignored )
            {
            }
        }

        if ( organisationUnit.getParent() != null )
        {
            facility.getProperties().put( "parent", organisationUnit.getParent().getUid() );
        }

        Identifier identifier = new Identifier();

        identifier.setAgency( Identifier.DHIS2_AGENCY );
        identifier.setContext( Identifier.DHIS2_UID_CONTEXT );
        identifier.setId( organisationUnit.getUid() );

        facility.getIdentifiers().add( identifier );

        if ( organisationUnit.getCode() != null )
        {
            identifier = new Identifier();
            identifier.setAgency( Identifier.DHIS2_AGENCY );
            identifier.setContext( Identifier.DHIS2_CODE_CONTEXT );
            identifier.setId( organisationUnit.getCode() );

            facility.getIdentifiers().add( identifier );
        }

        List<String> dataSets = new ArrayList<String>();

        for ( DataSet dataSet : organisationUnit.getDataSets() )
        {
            dataSets.add( dataSet.getUid() );
        }

        if ( !dataSets.isEmpty() )
        {
            facility.getProperties().put( "dataSets", dataSets );
        }

        facility.getProperties().put( "level", organisationUnit.getOrganisationUnitLevel() );

        return facility;
    }
}