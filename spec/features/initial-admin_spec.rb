require 'spec_helper'
require 'pry'

feature 'Initial admin', type: :feature do
  scenario 'Create an initial admin and sign works ' do
    visit '/'

    # we get redirected to the initial admin because there are no admins yet
    expect(page).to have_content  "Initial Admin"

    # we create the initial admin
    fill_in 'email', with: 'admin@example.com'
    fill_in 'password', with: 'password'
    click_on 'Create'

    # we sign-in as the admin
    fill_in 'email', with: 'admin@example.com'
    fill_in 'password', with: 'password'
    click_on 'Sign in'

    # we are signed-in
    expect(first('.user-nav img.user-img-32')).to be

    # the authentication method is session
    visit '/auth'

    wait_until {page.has_content? /authentication-method.+session/}

    find("#sign-out").click

    # we are signed-out
    expect(first('.user-nav img.user-img-32')).not_to be

  end
end
