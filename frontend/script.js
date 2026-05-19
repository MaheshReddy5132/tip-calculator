const form = document.getElementById('tip-form');
const amountEl = document.getElementById('amount');

form.addEventListener('submit', async (event) => {
  event.preventDefault();

  const billValue = parseFloat(document.getElementById('bill').value);
  const tipValue = parseFloat(document.getElementById('tip').value);
  const peopleValue = parseInt(document.getElementById('people').value, 10);

  if (Number.isNaN(billValue) || billValue <= 0 || Number.isNaN(tipValue) || tipValue < 0 || Number.isNaN(peopleValue) || peopleValue <= 0) {
    amountEl.textContent = '₹0.00';
    alert('Please enter valid numbers for bill, tip percent, and number of people.');
    return;
  }

  try {
    const response = await fetch('/calculate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        bill: billValue,
        tip: tipValue,
        people: peopleValue
      })
    });

    if (!response.ok) {
      throw new Error('Unable to calculate tip at this time.');
    }

    const data = await response.json();
    amountEl.textContent = `₹${data.eachPays.toFixed(2)}`;
  } catch (error) {
    amountEl.textContent = '₹0.00';
    alert(error.message || 'Something went wrong while calculating.');
  }
});
