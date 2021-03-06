package io.vgs.track.interceptor.transaction;

import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import io.vgs.track.BaseTest;
import io.vgs.track.data.EntityTrackingFieldData;
import io.vgs.track.meta.Trackable;
import io.vgs.track.meta.Tracked;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("Duplicates")
public class PersistentSetTest extends BaseTest {

  @Test
  public void shouldTrackPersistentSet() {
    Long addressId = doInJpa(em -> {
      Car car = new Car();
      Address address = new Address();

      car.setAddress(address);
      address.getCars().add(car);

      em.persist(address);
      em.persist(car);
      return address.getId();
    });

    clearContext();

    doInJpa(em -> {
      Address address = em.find(Address.class, addressId);
      Car newCar = new Car();

      address.getCars().add(newCar);
      newCar.setAddress(address);

      em.persist(newCar);
    });

    assertThat(testEntityTrackingListener.getUpdates().isEmpty(), is(false));
    EntityTrackingFieldData cars = testEntityTrackingListener.getUpdatedField("cars");
    assertThat(((Collection) cars.getOldValue()).size(), is(1));
    assertThat(((Collection) cars.getNewValue()).size(), is(2));
  }

  @Override
  protected Class<?>[] entities() {
    return new Class[]{
        Address.class,
        Car.class
    };
  }


  @Entity
  @Trackable
  @Tracked
  public static class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "address_seq")
    @SequenceGenerator(name = "address_seq", sequenceName = "address_seq")
    private Long id;

    @OneToMany(mappedBy = "address")
    private Set<Car> cars = new HashSet<>();

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Set<Car> getCars() {
      return cars;
    }

    public void setCars(Set<Car> cars) {
      this.cars = cars;
    }
  }

  @Entity
  @Trackable
  @Tracked
  public static class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_seq")
    @SequenceGenerator(name = "car_seq", sequenceName = "car_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_address")
    private Address address;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }
  }

}
