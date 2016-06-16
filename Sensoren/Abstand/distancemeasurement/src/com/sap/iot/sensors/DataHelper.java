package com.sap.iot.sensors;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.persistence.Measurement;
import org.persistence.Sensor;

public class DataHelper {
	private EntityManagerFactory emf;

	public DataHelper(EntityManagerFactory emf) {
		this.emf = emf;
	}

	/*
	 * Persists a measured sensor value (measurement)
	 * 
	 * @param measurement The measured sensor value
	 */
	public boolean addMeasurement(Measurement measurement) {
		boolean result = false;

		EntityManager em = emf.createEntityManager();
		// System.out.println("Trying to commit sensor data for sensor " +
		// measurement.getSensorDescription());
		try {
			if (measurement != null && measurement.getValue() != null) {
				em.getTransaction().begin();
				em.persist(measurement);
				em.getTransaction().commit();
			}
		} catch (Exception e) {
			System.out.println("ERROR: persisting measurement didn't work " + e.getMessage());
		} finally {
			em.close();
		}

		return result;
	}

	/*
	 * Persists a new sensor
	 * 
	 * @param sensor The sensor object to be added
	 */
	public boolean addSensor(Sensor sensor) {
		boolean result = false;
		if (sensor != null) {
			EntityManager em = emf.createEntityManager();
			try {

				em.getTransaction().begin();
				em.persist(sensor);
				em.getTransaction().commit();
				result = true;

			} catch (Exception e) {
				System.out.println("ERROR: persisting sensor didn't work " + e.getMessage());
				result = false;
			}
			em.close();

		}

		return result;
	}

	/*
	 * Provides a list of a defined number of sensor readings for a specific
	 * sensor. The method will provide the newest sensor readings (measurements)
	 * first
	 * 
	 * @param sensorId The sensor id of the sensor that you wish to get the
	 * measured values from
	 * 
	 * @param numberOfReadings The maximum number of readings you'll get back
	 */
	@SuppressWarnings("unchecked")
	public List<Measurement> getLastSensorReadings(long sensorId, int numberOfReadings) {
		List<Measurement> result = null;

		EntityManager em = emf.createEntityManager();
		try {

			Query q = em.createNamedQuery("LastReadingsFromSensor");
			q.setParameter("paramSensorId", sensorId);
			// To not affect performance we just retrieve the first 20 result
			// sets
			q.setMaxResults(numberOfReadings);
			result = q.getResultList();

			Collections.sort(result, new Comparator<Measurement>() {
				public int compare(Measurement m1, Measurement m2) {
					return m1.getStoredAt().compareTo(m2.getStoredAt());
				}
			});

		} catch (Exception e) {
		}

		em.close();
		return result;
	}

	/*
	 * Provides a list of ALL sensor readings. To avoid too many data the output
	 * is restricted to a maximum of 500 entries
	 */
	@SuppressWarnings("unchecked")
	public List<Measurement> getAllSensorReadings() {
		List<Measurement> result = null;

		EntityManager em = emf.createEntityManager();
		try {
			Query q = em.createNamedQuery("AllMeasurements");
			q.setMaxResults(500);
			result = q.getResultList();

		} catch (Exception e) {
		}

		em.close();
		return result;
	}

	/*
	 * Provides the last measured sensor value for a sensor
	 * 
	 * @param sensorId The sensor id of the sensor that you wish to get the
	 * measured value from
	 */
	public Measurement getLastSensorReading(long sensorId) {
		Measurement result = null;

		EntityManager em = emf.createEntityManager();
		try {
			Query q = em.createNamedQuery("LastSensorReading");
			q.setParameter("paramSensorId", sensorId);
			result = (Measurement) q.getSingleResult();

		} catch (Exception e) {
		}

		em.close();
		return result;
	}

	/*
	 * Provides a list of all sensors
	 */
	@SuppressWarnings("unchecked")
	public List<Sensor> getListOfSensors() {
		List<Sensor> result = null;

		EntityManager em = emf.createEntityManager();
		try {
			Query q = em.createNamedQuery("GetListOfSensors");
			result = q.getResultList();
		} catch (Exception e) {
		}
		
		em.close();
		return result;
	}
}