package io.vgs.track.interceptor.transaction;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import io.vgs.track.BaseTest;
import io.vgs.track.data.EntityTrackingData;
import io.vgs.track.data.EntityTrackingFieldData;
import io.vgs.track.meta.Trackable;
import io.vgs.track.meta.Tracked;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

@SuppressWarnings("Duplicates")
public class OneToManyUnidirectionalListTest extends BaseTest {
  @Test
  public void insertWithRightOrder() {
    doInJpa(em -> {
      Address address = new Address();
      Car car = new Car();

      address.getCars().add(car);

      // address - first, car - second
      em.persist(address);
      em.persist(car);
    });

    List<EntityTrackingData> inserts = testEntityTrackingListener.getInserts();
    assertThat(inserts, hasSize(1));

    EntityTrackingFieldData cars = testEntityTrackingListener.getInsertedField("cars");
    assertThat(cars.getOldValue(), is(nullValue()));
    assertThat(((Collection<?>) cars.getNewValue()), containsInAnyOrder(50L));
  }

  @Test
  public void insertWithWrongOrder() {
    doInJpa(em -> {
      Address address = new Address();
      Car car = new Car();

      address.getCars().add(car);

      // car - first, address - second
      // generates additional sql update:
      // update Car set id_address=? where id=?
      em.persist(car);
      em.persist(address);
    });

    List<EntityTrackingData> inserts = testEntityTrackingListener.getInserts();
    assertThat(inserts, hasSize(1));

    EntityTrackingFieldData cars = testEntityTrackingListener.getInsertedField("cars");
    assertThat(cars.getOldValue(), is(nullValue()));
    assertThat(((Collection<?>) cars.getNewValue()), containsInAnyOrder(50L));
  }

  @Entity
  @Trackable
  @Tracked
  public static class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_seq")
    @SequenceGenerator(name = "car_seq", sequenceName = "car_seq")
    private Long id;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

  }

  @Entity
  @Trackable
  @Tracked
  public static class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "address_seq")
    @SequenceGenerator(name = "address_seq", sequenceName = "address_seq")
    private Long id;

    @OneToMany
    private List<Car> cars = new ArrayList<>();

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public List<Car> getCars() {
      return cars;
    }

    public void setCars(List<Car> cars) {
      this.cars = cars;
    }
  }

  @Override
  protected Class<?>[] entities() {
    return new Class[]{
        Car.class,
        Address.class
    };
  }
}
