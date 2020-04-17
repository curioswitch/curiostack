/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.curioswitch.scrapers.instagram.server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.curioswitch.common.server.framework.immutables.CurioStyle;
import org.immutables.value.Value.Immutable;

/** The sharedData of a profile page. */
@Immutable
@CurioStyle
@JsonDeserialize(as = ImmutableProfilePage.class)
public interface ProfilePage {
  @Immutable
  @CurioStyle
  @JsonDeserialize(as = ImmutableTimeline.class)
  interface Timeline {
    @JsonProperty("edges")
    List<GraphImageEdge> getEdges();
  }

  @Immutable
  @CurioStyle
  @JsonDeserialize(as = ImmutableUser.class)
  interface User {
    @JsonProperty("edge_owner_to_timeline_media")
    Timeline getTimeline();
  }

  @Immutable
  @CurioStyle
  @JsonDeserialize(as = ImmutableProfilePageGraphql.class)
  interface ProfilePageGraphql {
    @JsonProperty("user")
    User getUser();
  }

  @Immutable
  @CurioStyle
  @JsonDeserialize(as = ImmutableProfilePageData.class)
  interface ProfilePageData {
    @JsonProperty("graphql")
    ProfilePageGraphql getGraphql();
  }

  @Immutable
  @CurioStyle
  @JsonDeserialize(as = ImmutableProfilePageEntryData.class)
  interface ProfilePageEntryData {
    @JsonProperty("ProfilePage")
    List<ProfilePageData> getProfilePage();
  }

  @JsonProperty("entry_data")
  ProfilePageEntryData getEntryData();
}
