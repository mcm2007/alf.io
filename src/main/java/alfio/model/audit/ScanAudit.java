/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.model.audit;

import alfio.manager.support.CheckInStatus;
import ch.digitalfondue.npjt.ConstructorAnnotationRowMapper.Column;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ScanAudit(
    @JsonProperty("ticketUuid") @Column("ticket_uuid") String ticketUuid,
    @JsonProperty("scanTimestamp") @Column("scan_ts") LocalDateTime scanTimestamp,
    @JsonProperty("username") @Column("username") String username,
    @JsonProperty("checkInStatus") @Column("check_in_status") CheckInStatus checkInStatus,
    @JsonProperty("operation") @Column("operation") ScanAudit.Operation operation) {

    public enum Operation {
        SCAN,
        REVERT
    }

    @JsonIgnore
    public String ticketUuid() {
        return ticketUuid;
    }
}
