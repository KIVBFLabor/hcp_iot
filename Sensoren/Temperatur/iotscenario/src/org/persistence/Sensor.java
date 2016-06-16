

    package org.persistence;

     

    import java.io.Serializable;

    import javax.persistence.*;

    import org.persistence.Measurement;

    import java.util.Collection;

    import static javax.persistence.GenerationType.AUTO;

     

    @Entity

    @NamedQueries({ @NamedQuery(name = "GetListOfSensors", query = "select s from Sensor s") })

    public class Sensor implements Serializable {

     

        private static final long serialVersionUID = 1L;

     

        public Sensor() { }

     

        @Id

        @GeneratedValue(strategy = AUTO)

        private long id;

        private String device;

        private String type;

        private String description;

        private Measurement lastMeasurement;

        @OneToMany

        private Collection<Measurement> measurements;

     

        public Measurement getLastMeasurement() {return lastMeasurement;}

        public void setLastMeasurement(Measurement lastMeasurement) {this.lastMeasurement = lastMeasurement;}

        public long getId() {return id;}

        public void setId(long id) {this.id = id;}

        public String getDevice() {return device;}

        public void setDevice(String param) {this.device = param;}

        public String getType() {return type;}

        public void setType(String param) {this.type = param;}

        public String getDescription() {return description;}

        public void setDescription(String param) {this.description = param;}

        public Collection<Measurement> getMeasurement() {return measurements;}

        public void setMeasurement(Collection<Measurement> param) {this.measurements = param;}

    }
